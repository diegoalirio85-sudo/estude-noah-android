package com.estudenoah.backend.api;

import com.estudenoah.backend.material.MaterialUrlResolution;
import com.estudenoah.backend.material.MaterialUrlResolver;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class MaterialUrlController {
    private final MaterialUrlResolver resolver;

    public MaterialUrlController(MaterialUrlResolver resolver) {
        this.resolver = resolver;
    }

    @PostMapping(
            path = "/v1/materials/url/resolve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public MaterialUrlResolution resolve(@RequestBody(required = false) MaterialUrlRequest request) {
        return resolver.resolve(request == null ? null : request.url());
    }

    public record MaterialUrlRequest(String url) {
    }
}
