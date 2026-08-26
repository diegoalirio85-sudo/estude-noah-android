package com.estudenoah.backend.material;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFSlideShowImpl;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.springframework.stereotype.Service;

@Service
public final class LegacyPptExtractor {
    private static final int MINIMUM_USABLE_CHARACTERS = 140;

    public PptExtractionResult extract(String fileName, InputStream input) {
        try (HSLFSlideShow slideShow = new HSLFSlideShow(new HSLFSlideShowImpl(input))) {
            List<PptSlide> slides = new ArrayList<>();
            for (int index = 0; index < slideShow.getSlides().size(); index++) {
                var slide = slideShow.getSlides().get(index);
                String slideText = slide.getTextParagraphs().stream()
                        .map(HSLFTextParagraph::getText)
                        .map(LegacyPptExtractor::normalize)
                        .filter(text -> !text.isBlank())
                        .collect(Collectors.joining("\n"));
                slides.add(new PptSlide(index + 1, slideText));
            }

            String structuredText = slides.stream()
                    .map(slide -> "Slide " + slide.number()
                            + (slide.text().isBlank() ? "" : "\n" + slide.text()))
                    .collect(Collectors.joining("\n\n"));
            int extractedCharacterCount = slides.stream()
                    .mapToInt(slide -> slide.text().length())
                    .sum();

            return new PptExtractionResult(
                    safeFileName(fileName),
                    slides.size(),
                    structuredText,
                    slides,
                    extractedCharacterCount >= MINIMUM_USABLE_CHARACTERS
            );
        } catch (EncryptedDocumentException error) {
            throw new PptExtractionException("O arquivo PPT está protegido e não pode ser processado.", error);
        } catch (OfficeXmlFileException error) {
            throw new PptExtractionException("O arquivo enviado não é um PowerPoint binário .ppt/.pps.", error);
        } catch (IOException | RuntimeException error) {
            throw new PptExtractionException("O arquivo PPT é inválido, corrompido ou não suportado.", error);
        }
    }

    private static String normalize(String raw) {
        return raw
                .replace('\u0000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n[ \\t]+", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    private static String safeFileName(String fileName) {
        String candidate = fileName == null || fileName.isBlank() ? "material.ppt" : fileName;
        candidate = candidate.replace('\\', '/');
        return candidate.substring(candidate.lastIndexOf('/') + 1);
    }
}
