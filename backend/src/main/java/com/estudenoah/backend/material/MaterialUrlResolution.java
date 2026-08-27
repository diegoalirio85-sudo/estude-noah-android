package com.estudenoah.backend.material;

public record MaterialUrlResolution(
        String kind,
        String inputUrl,
        String resolvedUrl,
        String title
) {
}
