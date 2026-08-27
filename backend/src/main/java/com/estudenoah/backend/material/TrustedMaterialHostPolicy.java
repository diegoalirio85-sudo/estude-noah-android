package com.estudenoah.backend.material;

import com.estudenoah.backend.api.ApiException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class TrustedMaterialHostPolicy {
    private static final Set<String> YOUTUBE_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be", "www.youtube-nocookie.com"
    );

    private final Set<String> exactHosts;
    private final Set<String> wildcardSuffixes;

    public TrustedMaterialHostPolicy(
            @Value("${material-url.allowed-hosts:brasilescola.uol.com.br,*.jesuitasbrasil.org.br}") String configuredHosts
    ) {
        Set<String> entries = Arrays.stream(configuredHosts.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        this.exactHosts = entries.stream()
                .filter(value -> !value.startsWith("*."))
                .collect(Collectors.toUnmodifiableSet());
        this.wildcardSuffixes = entries.stream()
                .filter(value -> value.startsWith("*."))
                .map(value -> value.substring(1))
                .collect(Collectors.toUnmodifiableSet());
    }

    public URI parseInput(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw invalid("Informe o link do material da escola.");
        }
        final URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException error) {
            throw invalid("O link do material é inválido.");
        }
        validateBasic(uri);
        if (!isYoutube(uri) && !isTrustedPage(uri)) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "material_url_untrusted_host",
                    "Este domínio ainda não está autorizado para resolução automática."
            );
        }
        return uri;
    }

    public void requireTrustedPage(URI uri) {
        validateBasic(uri);
        if (!isTrustedPage(uri)) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "material_url_untrusted_host",
                    "O redirecionamento saiu dos domínios escolares autorizados."
            );
        }
    }

    public boolean isTrustedPage(URI uri) {
        String host = normalizedHost(uri);
        if (host == null) return false;
        if (exactHosts.contains(host)) return true;
        return wildcardSuffixes.stream().anyMatch(host::endsWith);
    }

    public boolean isYoutube(URI uri) {
        String host = normalizedHost(uri);
        return host != null && YOUTUBE_HOSTS.contains(host);
    }

    private void validateBasic(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getPort() != -1) {
            throw invalid("Use um link HTTPS sem credenciais ou porta personalizada.");
        }
    }

    private static String normalizedHost(URI uri) {
        return uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_material_url", message);
    }
}
