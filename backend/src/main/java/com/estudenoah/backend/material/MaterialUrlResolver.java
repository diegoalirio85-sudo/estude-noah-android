package com.estudenoah.backend.material;

import com.estudenoah.backend.api.ApiException;
import com.estudenoah.backend.video.YoutubeUrlNormalizer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class MaterialUrlResolver {
    private static final int MAX_PAGES = 4;
    private static final Pattern EMBED_PATH = Pattern.compile("^/embed/([A-Za-z0-9_-]{11})");
    private static final Pattern META_URL = Pattern.compile("(?i)(?:^|;)\\s*url\\s*=\\s*['\"]?([^'\";]+)");

    private final TrustedMaterialHostPolicy policy;
    private final MaterialPageFetcher fetcher;
    private final YoutubeUrlNormalizer youtubeNormalizer;

    public MaterialUrlResolver(
            TrustedMaterialHostPolicy policy,
            MaterialPageFetcher fetcher,
            YoutubeUrlNormalizer youtubeNormalizer
    ) {
        this.policy = policy;
        this.fetcher = fetcher;
        this.youtubeNormalizer = youtubeNormalizer;
    }

    public MaterialUrlResolution resolve(String rawUrl) {
        URI input = policy.parseInput(rawUrl);
        Optional<URI> direct = normalizeYoutubeCandidate(input.toString());
        if (direct.isPresent()) {
            URI youtube = direct.get();
            return new MaterialUrlResolution("youtube", input.toString(), youtube.toString(), "Vídeo do YouTube");
        }

        Queue<URI> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(input);
        String bestTitle = "Material da escola";
        int processed = 0;

        while (!pending.isEmpty() && processed < MAX_PAGES) {
            URI pageUri = pending.remove();
            if (!visited.add(pageUri.normalize().toString())) continue;
            processed++;

            FetchedMaterialPage page = fetcher.fetch(pageUri);
            Optional<URI> redirectedYoutube = normalizeYoutubeCandidate(page.finalUri().toString());
            if (redirectedYoutube.isPresent()) {
                return new MaterialUrlResolution("youtube", input.toString(), redirectedYoutube.get().toString(), bestTitle);
            }

            Document document = Jsoup.parse(page.body(), page.finalUri().toString());
            if (!document.title().isBlank()) bestTitle = document.title().trim();
            if (looksLikeLoginPage(page.finalUri(), document)) {
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "material_url_login_required",
                        "Este link exige uma sessão autenticada do site escolar."
                );
            }

            Optional<URI> embeddedYoutube = findYoutube(document);
            if (embeddedYoutube.isPresent()) {
                return new MaterialUrlResolution("youtube", input.toString(), embeddedYoutube.get().toString(), bestTitle);
            }

            enqueueTrustedExternalLinks(document, page.finalUri(), pending, visited);
        }

        throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "youtube_not_found_in_material_url",
                "Não encontrei um vídeo público do YouTube nesse link escolar."
        );
    }

    private Optional<URI> findYoutube(Document document) {
        for (Element element : document.select("[href], [src]")) {
            for (String attribute : new String[]{"href", "src"}) {
                if (!element.hasAttr(attribute)) continue;
                String absolute = element.absUrl(attribute);
                Optional<URI> normalized = normalizeYoutubeCandidate(absolute);
                if (normalized.isPresent()) return normalized;
            }
        }
        return Optional.empty();
    }

    private void enqueueTrustedExternalLinks(
            Document document,
            URI current,
            Queue<URI> pending,
            Set<String> visited
    ) {
        String currentHost = current.getHost() == null ? "" : current.getHost().toLowerCase(Locale.ROOT);
        for (Element anchor : document.select("a[href]")) {
            URI candidate = parseQuietly(anchor.absUrl("href"));
            if (!isHttpsTrustedPage(candidate)) continue;
            String candidateHost = candidate.getHost().toLowerCase(Locale.ROOT);
            if (candidateHost.equals(currentHost)) continue;
            if (!visited.contains(candidate.normalize().toString())) pending.add(candidate);
        }

        for (Element meta : document.select("meta[http-equiv=refresh][content]")) {
            Matcher matcher = META_URL.matcher(meta.attr("content"));
            if (!matcher.find()) continue;
            URI candidate = resolveQuietly(current, matcher.group(1).trim());
            if (isHttpsTrustedPage(candidate) && !visited.contains(candidate.normalize().toString())) {
                pending.add(candidate);
            }
        }
    }

    private boolean looksLikeLoginPage(URI uri, Document document) {
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("/login/") || path.endsWith("/login") || path.contains("login/index.php")) return true;
        if (!document.select("form[action*=login], input[type=password]").isEmpty()) return true;
        String title = document.title().toLowerCase(Locale.ROOT);
        return title.contains("login") || title.contains("entrar no site");
    }

    private boolean isHttpsTrustedPage(URI candidate) {
        return candidate != null
                && "https".equalsIgnoreCase(candidate.getScheme())
                && candidate.getHost() != null
                && candidate.getUserInfo() == null
                && candidate.getPort() == -1
                && policy.isTrustedPage(candidate);
    }

    private Optional<URI> normalizeYoutubeCandidate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        URI candidate = parseQuietly(raw.replace("&amp;", "&"));
        if (candidate == null || !"https".equalsIgnoreCase(candidate.getScheme()) || candidate.getHost() == null) {
            return Optional.empty();
        }
        String host = candidate.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("www.youtube.com") || host.equals("youtube.com") || host.equals("m.youtube.com")) {
            Matcher embed = EMBED_PATH.matcher(candidate.getPath() == null ? "" : candidate.getPath());
            if (embed.find()) {
                return Optional.of(URI.create("https://www.youtube.com/watch?v=" + embed.group(1)));
            }
        }
        if (host.equals("www.youtube-nocookie.com")) {
            Matcher embed = EMBED_PATH.matcher(candidate.getPath() == null ? "" : candidate.getPath());
            if (embed.find()) {
                return Optional.of(URI.create("https://www.youtube.com/watch?v=" + embed.group(1)));
            }
            return Optional.empty();
        }
        try {
            return Optional.of(youtubeNormalizer.normalize(candidate.toString()));
        } catch (ApiException ignored) {
            return Optional.empty();
        }
    }

    private static URI resolveQuietly(URI base, String raw) {
        try {
            return base.resolve(raw);
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    private static URI parseQuietly(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new URI(raw.trim());
        } catch (URISyntaxException error) {
            return null;
        }
    }
}
