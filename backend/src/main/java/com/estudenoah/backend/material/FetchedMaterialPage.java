package com.estudenoah.backend.material;

import java.net.URI;

public record FetchedMaterialPage(
        URI finalUri,
        String contentType,
        String body
) {
}
