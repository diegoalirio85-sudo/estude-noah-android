package com.estudenoah.backend.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.estudenoah.backend.api.ApiException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class TrustedMaterialHostPolicyTest {
    private final TrustedMaterialHostPolicy policy = new TrustedMaterialHostPolicy(
            "brasilescola.uol.com.br,*.jesuitasbrasil.org.br"
    );

    @Test
    void acceptsConfiguredSchoolHostsAndYoutube() {
        assertThat(policy.parseInput("https://brasilescola.uol.com.br/videos/a.htm").getHost())
                .isEqualTo("brasilescola.uol.com.br");
        assertThat(policy.parseInput("https://avarje.jesuitasbrasil.org.br/mod/url/view.php?id=1").getHost())
                .isEqualTo("avarje.jesuitasbrasil.org.br");
        assertThat(policy.parseInput("https://www.youtube.com/watch?v=AbCdEf123_-").getHost())
                .isEqualTo("www.youtube.com");
    }

    @Test
    void rejectsLookalikesAndUnsafeSchemes() {
        assertThatThrownBy(() -> policy.parseInput("https://brasilescola.uol.com.br.evil.example/video"))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("material_url_untrusted_host");
        assertThatThrownBy(() -> policy.parseInput("http://brasilescola.uol.com.br/video"))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("invalid_material_url");
        assertThatThrownBy(() -> policy.parseInput("https://user:pass@brasilescola.uol.com.br/video"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> policy.requireTrustedPage(URI.create("https://127.0.0.1/internal")))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("material_url_untrusted_host");
    }
}
