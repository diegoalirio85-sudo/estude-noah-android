package com.estudenoah.backend.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.estudenoah.backend.api.ApiException;
import com.estudenoah.backend.video.YoutubeUrlNormalizer;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaterialUrlResolverTest {
    private final TrustedMaterialHostPolicy policy = new TrustedMaterialHostPolicy(
            "brasilescola.uol.com.br,*.jesuitasbrasil.org.br"
    );

    @Test
    void returnsCanonicalUrlForDirectYoutubeLink() {
        MaterialUrlResolver resolver = resolver(Map.of());

        MaterialUrlResolution result = resolver.resolve("https://youtu.be/AbCdEf123_-");

        assertThat(result.kind()).isEqualTo("youtube");
        assertThat(result.resolvedUrl()).isEqualTo("https://www.youtube.com/watch?v=AbCdEf123_-");
    }

    @Test
    void extractsBrasilEscolaYoutubeEmbed() {
        String source = "https://brasilescola.uol.com.br/videos/brasil-colonia-o-inicio.htm";
        String html = """
                <html><head><title>Videoaula sobre Brasil Colônia | O início</title></head>
                <body><iframe src="https://www.youtube.com/embed/ocjJ8bKEQ3Q&mute=1"></iframe></body></html>
                """;
        MaterialUrlResolver resolver = resolver(Map.of(source, html));

        MaterialUrlResolution result = resolver.resolve(source);

        assertThat(result.resolvedUrl()).isEqualTo("https://www.youtube.com/watch?v=ocjJ8bKEQ3Q");
        assertThat(result.title()).contains("Brasil Colônia");
    }

    @Test
    void followsTrustedExternalSchoolLinkBeforeFindingYoutube() {
        String ava = "https://avarje.jesuitasbrasil.org.br/antoniovieiraba/mod/url/view.php?id=45958";
        String brasilEscola = "https://brasilescola.uol.com.br/videos/brasil-colonia-o-inicio.htm";
        Map<String, String> pages = new HashMap<>();
        pages.put(ava, "<html><body><a href=\"" + brasilEscola + "\">Abrir recurso</a></body></html>");
        pages.put(brasilEscola, "<html><head><title>Brasil Colônia</title></head><body>"
                + "<a href=\"https://www.youtube.com/watch?v=ocjJ8bKEQ3Q\">Videoaula</a></body></html>");
        MaterialUrlResolver resolver = resolver(pages);

        MaterialUrlResolution result = resolver.resolve(ava);

        assertThat(result.resolvedUrl()).isEqualTo("https://www.youtube.com/watch?v=ocjJ8bKEQ3Q");
        assertThat(result.title()).isEqualTo("Brasil Colônia");
    }

    @Test
    void reportsAvaLoginPageInsteadOfPretendingContentWasResolved() {
        String ava = "https://avarje.jesuitasbrasil.org.br/login/index.php";
        MaterialUrlResolver resolver = resolver(Map.of(
                ava,
                "<html><head><title>Entrar no site</title></head><body><form action=\"/login/index.php\"><input type=\"password\"></form></body></html>"
        ));

        assertThatThrownBy(() -> resolver.resolve(ava))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("material_url_login_required");
    }

    @Test
    void rejectsTrustedPageWithoutYoutube() {
        String source = "https://brasilescola.uol.com.br/videos/sem-video.htm";
        MaterialUrlResolver resolver = resolver(Map.of(source, "<html><body>Somente texto.</body></html>"));

        assertThatThrownBy(() -> resolver.resolve(source))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("youtube_not_found_in_material_url");
    }

    private MaterialUrlResolver resolver(Map<String, String> pages) {
        MaterialPageFetcher fetcher = uri -> {
            String html = pages.get(uri.toString());
            if (html == null) throw new AssertionError("Unexpected fetch: " + uri);
            return new FetchedMaterialPage(uri, "text/html; charset=utf-8", html);
        };
        return new MaterialUrlResolver(policy, fetcher, new YoutubeUrlNormalizer());
    }
}
