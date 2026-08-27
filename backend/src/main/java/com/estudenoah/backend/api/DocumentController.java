package com.estudenoah.backend.api;

import com.estudenoah.backend.activity.GeneratedActivity;
import com.estudenoah.backend.document.DocumentActivityPipelineService;
import com.estudenoah.backend.document.DocumentAnalysis;
import com.estudenoah.backend.document.DocumentAnalysisRequest;
import com.estudenoah.backend.document.DocumentAnalysisService;
import com.estudenoah.backend.material.LegacyPptExtractor;
import com.estudenoah.backend.material.PptExtractionException;
import java.io.IOException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public final class DocumentController {
    private final DocumentAnalysisService analysisService;
    private final DocumentActivityPipelineService pipelineService;
    private final LegacyPptExtractor pptExtractor;

    public DocumentController(DocumentAnalysisService analysisService, DocumentActivityPipelineService pipelineService,
                              LegacyPptExtractor pptExtractor) {
        this.analysisService = analysisService;
        this.pipelineService = pipelineService;
        this.pptExtractor = pptExtractor;
    }

    @PostMapping(path = "/v1/materials/text/analyze", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DocumentAnalysis analyze(@RequestBody(required = false) DocumentAnalysisRequest request) {
        return analysisService.analyze(request);
    }

    @PostMapping(path = "/v1/activities/from-text", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GeneratedActivity fromText(@RequestBody(required = false) DocumentAnalysisRequest request) {
        return pipelineService.generate(request);
    }

    @PostMapping(path = "/v1/activities/from-ppt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GeneratedActivity fromPpt(@RequestPart("file") MultipartFile file, @RequestParam("subject") String subject,
                                     @RequestParam("grade") String grade) {
        String fileName = file.getOriginalFilename();
        if (file.isEmpty()) throw api(HttpStatus.BAD_REQUEST, "empty_file", "Envie um arquivo PPT/PPS não vazio.");
        String normalizedName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (!normalizedName.endsWith(".ppt") && !normalizedName.endsWith(".pps"))
            throw api(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_file_type", "Este endpoint aceita somente PowerPoint binário com extensão .ppt ou .pps.");
        try {
            var extraction = pptExtractor.extract(fileName, file.getInputStream());
            if (!extraction.usableForGeneration())
                throw api(HttpStatus.UNPROCESSABLE_CONTENT, "insufficient_ppt_text", "O PPT não contém texto suficiente para gerar uma atividade.");
            return pipelineService.generate(new DocumentAnalysisRequest("ppt", extraction.fileName(), subject, grade, extraction.text()));
        } catch (PptExtractionException error) {
            throw api(HttpStatus.UNPROCESSABLE_ENTITY, "ppt_extraction_failed", error.getMessage());
        } catch (IOException error) {
            throw api(HttpStatus.BAD_REQUEST, "upload_read_failed", "Não foi possível ler o upload.");
        }
    }

    private static ApiException api(HttpStatus status, String code, String message) { return new ApiException(status, code, message); }
}
