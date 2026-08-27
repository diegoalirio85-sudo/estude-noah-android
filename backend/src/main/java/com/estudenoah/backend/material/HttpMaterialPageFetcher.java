package com.estudenoah.backend.material;

import com.estudenoah.backend.api.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public final class HttpMaterialPageFetcher implements MaterialPageFetcher {
    private static final int MAX_REDIRECTS = 4;
    private static final int MAX_HTML_BYTES = 2 * 1024 * 1024;
    private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);

    private final TrustedMaterialHostPolicy policy;
    private final HttpClient client;

    public HttpMaterialPageFetcher(TrustedMaterialHostPolicy policy) {
        this.policy = policy;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public FetchedMaterialPage fetch(URI initialUri) {
        URI current = initialUri;
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            if (policy.isYoutube(current)) {
                return new FetchedMaterialPage(current, "", "");
            }
            policy.requireTrustedPage(current);

            HttpRequest request = HttpRequest.newBuilder(current)
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("User-Agent", "EstudeNoah/1.0 educational-material-resolver")
                    .build();
            final HttpResponse<InputStream> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw upstream("A resolução do link foi interrompida.");
            } catch (IOException error) {
                throw upstream("Não foi possível abrir o link do material.");
            }

            int status = response.statusCode();
            if (REDIRECT_STATUSES.contains(status)) {
                closeQuietly(response.body());
                String location = response.headers().firstValue("location").orElse(null);
                if (location == null || location.isBlank()) {
                    throw upstream("O material retornou um redirecionamento inválido.");
                }
                current = current.resolve(location.trim());
                if (!policy.isYoutube(current)) policy.requireTrustedPage(current);
                continue;
            }

            if (status == 401 || status == 403) {
                closeQuietly(response.body());
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "material_url_login_required",
                        "Este link exige uma sessão autenticada do site escolar."
                );
            }
            if (status < 200 || status >= 300) {
                closeQuietly(response.body());
                throw upstream("O site do material respondeu com HTTP " + status + ".");
            }

            String contentType = response.headers().firstValue("content-type").orElse("");
            String normalizedType = contentType.toLowerCase();
            if (!normalizedType.startsWith("text/html") && !normalizedType.startsWith("application/xhtml+xml")) {
                closeQuietly(response.body());
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "material_url_unsupported_content",
                        "O link não aponta para uma página HTML compatível com esta etapa."
                );
            }

            try (InputStream input = response.body()) {
                return new FetchedMaterialPage(current, contentType, readLimited(input));
            } catch (IOException error) {
                throw upstream("Não foi possível ler a página do material.");
            }
        }
        throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "material_url_too_many_redirects",
                "O link passou por redirecionamentos demais."
        );
    }

    private static String readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        while (true) {
            int read = input.read(buffer);
            if (read < 0) break;
            total += read;
            if (total > MAX_HTML_BYTES) {
                throw new ApiException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "material_url_page_too_large",
                        "A página do material é grande demais para resolução automática."
                );
            }
            output.write(buffer, 0, read);
        }
        return output.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void closeQuietly(InputStream input) {
        if (input == null) return;
        try {
            input.close();
        } catch (IOException ignored) {
        }
    }

    private static ApiException upstream(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "material_url_upstream_error", message);
    }
}
