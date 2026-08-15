package com.estudenoah.backend.api;

import com.estudenoah.backend.material.LegacyPptExtractor;
import com.estudenoah.backend.material.PptExtractionException;
import com.estudenoah.backend.material.PptExtractionResult;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public final class MaterialController {
    private final LegacyPptExtractor extractor;

    public MaterialController(LegacyPptExtractor extractor) {
        this.extractor = extractor;
    }

    @PostMapping(
            path = "/v1/materials/ppt/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PptExtractionResult extractPpt(@RequestPart("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "empty_file", "Envie um arquivo PPT não vazio.");
        }
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".ppt")) {
            throw new ApiException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "unsupported_file_type",
                    "Este endpoint aceita somente PowerPoint binário com extensão .ppt."
            );
        }

        try {
            return extractor.extract(fileName, file.getInputStream());
        } catch (PptExtractionException error) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ppt_extraction_failed", error.getMessage());
        } catch (IOException error) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "upload_read_failed", "Não foi possível ler o upload.");
        }
    }
}
