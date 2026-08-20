package com.estudenoah.backend.video;

import com.estudenoah.backend.api.ApiException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class YoutubeUrlNormalizer {
    private static final Set<String> WATCH_HOSTS = Set.of("youtube.com", "www.youtube.com", "m.youtube.com");
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    public URI normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw invalid("Informe uma URL pública do YouTube.");
        }
        final URI input;
        try {
            input = new URI(rawUrl.trim());
        } catch (URISyntaxException error) {
            throw invalid("A URL do YouTube é inválida.");
        }
        if (!"https".equalsIgnoreCase(input.getScheme()) || input.getUserInfo() != null
                || input.getPort() != -1 || input.getHost() == null || input.getFragment() != null) {
            throw invalid("Use uma URL HTTPS oficial do YouTube.");
        }

        String host = input.getHost().toLowerCase(Locale.ROOT);
        String videoId;
        if (WATCH_HOSTS.contains(host)) {
            if (!"/watch".equals(input.getPath())) {
                throw invalid("Use uma URL pública youtube.com/watch.");
            }
            try {
                videoId = queryParameter(input.getRawQuery(), "v");
            } catch (IllegalArgumentException error) {
                throw invalid("A URL do YouTube é inválida.");
            }
        } else if ("youtu.be".equals(host)) {
            String path = input.getPath();
            videoId = path != null && path.matches("/[A-Za-z0-9_-]{11}") ? path.substring(1) : null;
        } else {
            throw invalid("Somente domínios oficiais youtube.com e youtu.be são aceitos.");
        }

        if (videoId == null || !VIDEO_ID.matcher(videoId).matches()) {
            throw invalid("A URL não contém um identificador válido de vídeo do YouTube.");
        }
        return URI.create("https://www.youtube.com/watch?v=" + videoId);
    }

    private static String queryParameter(String rawQuery, String name) {
        if (rawQuery == null) return null;
        String found = null;
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            String key = URLDecoder.decode(separator < 0 ? pair : pair.substring(0, separator), StandardCharsets.UTF_8);
            if (name.equals(key)) {
                if (found != null) return null;
                found = URLDecoder.decode(separator < 0 ? "" : pair.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return found;
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_youtube_url", message);
    }
}
