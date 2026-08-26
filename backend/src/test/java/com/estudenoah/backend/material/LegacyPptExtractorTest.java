package com.estudenoah.backend.material;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.junit.jupiter.api.Test;

class LegacyPptExtractorTest {
    private final LegacyPptExtractor extractor = new LegacyPptExtractor();

    @Test
    void extractsOneSlideWithTitleAndText() throws Exception {
        byte[] ppt = presentation(
                new String[]{"Sistema Solar", "Os planetas orbitam o Sol."}
        );

        PptExtractionResult result = extractor.extract("aula.ppt", new ByteArrayInputStream(ppt));

        assertThat(result.fileName()).isEqualTo("aula.ppt");
        assertThat(result.slideCount()).isEqualTo(1);
        assertThat(result.slides().getFirst().number()).isEqualTo(1);
        assertThat(result.slides().getFirst().text())
                .contains("Sistema Solar")
                .contains("Os planetas orbitam o Sol.");
        assertThat(result.text()).startsWith("Slide 1");
    }

    @Test
    void extractsRealBinaryPpsPresentationWithHslf() throws Exception {
        byte[] pps = presentation(new String[]{"Sistema Solar", "Conteúdo didático ".repeat(12)});
        System.out.println("SYNTHETIC_PPS_FIXTURE_BASE64=" + java.util.Base64.getEncoder().encodeToString(pps));

        PptExtractionResult result = extractor.extract("aula.pps", new ByteArrayInputStream(pps));

        assertThat(result.fileName()).isEqualTo("aula.pps");
        assertThat(result.slideCount()).isEqualTo(1);
        assertThat(result.text()).contains("Sistema Solar").contains("Conteúdo didático");
        assertThat(result.usableForGeneration()).isTrue();
    }

    @Test
    void preservesOrderAcrossMultipleSlides() throws Exception {
        byte[] ppt = presentation(
                new String[]{"Primeiro slide"},
                new String[]{"Segundo slide"},
                new String[]{"Terceiro slide"}
        );

        PptExtractionResult result = extractor.extract("ordem.ppt", new ByteArrayInputStream(ppt));

        assertThat(result.slideCount()).isEqualTo(3);
        assertThat(result.slides()).extracting(PptSlide::text)
                .containsExactly("Primeiro slide", "Segundo slide", "Terceiro slide");
        assertThat(result.text()).containsSubsequence("Slide 1", "Slide 2", "Slide 3");
    }

    @Test
    void reportsPresentationWithoutTextAsNotUsable() throws Exception {
        byte[] ppt = presentation(new String[]{});

        PptExtractionResult result = extractor.extract("vazio.ppt", new ByteArrayInputStream(ppt));

        assertThat(result.slideCount()).isEqualTo(1);
        assertThat(result.slides().getFirst().text()).isEmpty();
        assertThat(result.usableForGeneration()).isFalse();
    }

    @Test
    void rejectsInvalidOrCorruptedInput() {
        assertThatThrownBy(() -> extractor.extract(
                "invalido.ppt",
                new ByteArrayInputStream("isto não é um ppt".getBytes())
        )).isInstanceOf(PptExtractionException.class)
          .hasMessageContaining("inválido");
    }

    @Test
    void marksSubstantialExtractedTextAsUsable() throws Exception {
        String text = "Conteúdo didático artificial para teste. ".repeat(8);
        byte[] ppt = presentation(new String[]{text});

        PptExtractionResult result = extractor.extract("material.ppt", new ByteArrayInputStream(ppt));

        assertThat(result.usableForGeneration()).isTrue();
    }

    private static byte[] presentation(String[]... slides) throws Exception {
        try (HSLFSlideShow slideShow = new HSLFSlideShow();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String[] textBoxes : slides) {
                var slide = slideShow.createSlide();
                for (String text : textBoxes) {
                    HSLFTextBox box = new HSLFTextBox();
                    box.setText(text);
                    slide.addShape(box);
                }
            }
            slideShow.write(output);
            return output.toByteArray();
        }
    }
}
