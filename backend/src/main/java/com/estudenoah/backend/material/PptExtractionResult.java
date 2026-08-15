package com.estudenoah.backend.material;

import java.util.List;

public record PptExtractionResult(
        String fileName,
        int slideCount,
        String text,
        List<PptSlide> slides,
        boolean usableForGeneration
) {
    public PptExtractionResult {
        slides = List.copyOf(slides);
    }
}
