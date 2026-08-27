package com.estudenoah.backend.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.estudenoah.backend.material.LegacyPptExtractor;
import com.estudenoah.backend.activity.ActivityGenerationService;
import com.estudenoah.backend.video.VideoAnalysisService;
import com.estudenoah.backend.video.YoutubeUrlNormalizer;
import com.estudenoah.backend.document.DocumentActivityPipelineService;
import com.estudenoah.backend.document.DocumentAnalysis;
import com.estudenoah.backend.document.DocumentAnalysisProvider;
import com.estudenoah.backend.document.GeminiDocumentAnalysisService;
import com.estudenoah.backend.material.PptExtractionResult;
import com.estudenoah.backend.material.PptExtractionException;
import com.estudenoah.backend.video.VideoAnalysis;
import com.estudenoah.backend.security.AppCheckTokenVerifier;
import com.estudenoah.backend.security.FirebaseAuthTokenVerifier;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(properties = {"security.app-check.enabled=false", "security.auth.mode=none", "security.auth.allow-none=true"}, value = {HealthController.class, MaterialController.class, YoutubeMaterialController.class, ActivityController.class,
        DocumentController.class, DocumentActivityPipelineService.class, GeminiDocumentAnalysisService.class,
        YoutubeUrlNormalizer.class, ApiExceptionHandler.class})
class BackendApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LegacyPptExtractor extractor;

    @MockitoBean
    private VideoAnalysisService videoAnalysisService;

    @MockitoBean
    private ActivityGenerationService activityGenerationService;

    @MockitoBean
    private DocumentAnalysisProvider documentAnalysisProvider;

    @MockitoBean
    private AppCheckTokenVerifier appCheckTokenVerifier;

    @MockitoBean
    private FirebaseAuthTokenVerifier firebaseAuthTokenVerifier;

    @Test
    void healthDoesNotDependOnExternalServices() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("estude-noah-backend"));
    }

    @Test
    void rejectsUnsupportedExtension() throws Exception {
        var upload = new MockMultipartFile("file", "material.pptx", "application/octet-stream", new byte[]{1});

        mockMvc.perform(multipart("/v1/materials/ppt/extract").file(upload))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("unsupported_file_type"));
    }

    @Test
    void rejectsEmptyUpload() throws Exception {
        var upload = new MockMultipartFile("file", "material.ppt", "application/octet-stream", new byte[]{});

        mockMvc.perform(multipart("/v1/materials/ppt/extract").file(upload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("empty_file"));
    }

    @Test
    void rejectsUnsafeYoutubeDomainBeforeCallingProvider() throws Exception {
        mockMvc.perform(post("/v1/materials/youtube/analyze")
                        .contentType("application/json")
                        .content("{\"url\":\"https://youtube.com.evil.example/watch?v=AbCdEf123_-\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_youtube_url"));
    }

    @Test
    void rejectsMissingYoutubeUrl() throws Exception {
        mockMvc.perform(post("/v1/materials/youtube/analyze")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_youtube_url"));
    }

    @Test
    void activityEndpointAcceptsJsonAndDelegatesToService() throws Exception {
        mockMvc.perform(post("/v1/activities/generate")
                        .contentType("application/json")
                        .content("{\"grade\":\"4º Ano\",\"subject\":\"Ciências\",\"source\":{\"type\":\"youtube\",\"title\":\"Aula\",\"url\":\"https://youtu.be/AbCdEf123_-\"},\"analysis\":null}"))
                .andExpect(status().isOk());
    }

    @Test
    void analyzesValidPdfText() throws Exception {
        when(documentAnalysisProvider.analyze(any())).thenReturn(documentAnalysis("pdf"));
        mockMvc.perform(post("/v1/materials/text/analyze").contentType("application/json").content(documentJson("pdf")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceType").value("pdf"))
                .andExpect(jsonPath("$.themes[0].name").value("Tema"));
    }

    @Test
    void rejectsEmptyAndInsufficientDocumentText() throws Exception {
        mockMvc.perform(post("/v1/materials/text/analyze").contentType("application/json")
                        .content("{\"sourceType\":\"pdf\",\"sourceTitle\":\"Aula\",\"subject\":\"Ciências\",\"grade\":\"4º Ano\",\"text\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("empty_document_text"));
        mockMvc.perform(post("/v1/materials/text/analyze").contentType("application/json")
                        .content("{\"sourceType\":\"pdf\",\"sourceTitle\":\"Aula\",\"subject\":\"Ciências\",\"grade\":\"4º Ano\",\"text\":\"texto curto\"}"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("insufficient_document_text"));
    }

    @Test
    void generatesActivityFromTextThroughPipeline() throws Exception {
        when(documentAnalysisProvider.analyze(any())).thenReturn(documentAnalysis("docx"));
        mockMvc.perform(post("/v1/activities/from-text").contentType("application/json").content(documentJson("docx")))
                .andExpect(status().isOk());
    }

    @Test
    void generatesActivityFromValidLegacyPpt() throws Exception {
        String text = "conteúdo pedagógico ".repeat(12);
        when(extractor.extract(any(), any())).thenReturn(new PptExtractionResult("aula.ppt", 1, text, List.of(), true));
        when(documentAnalysisProvider.analyze(any())).thenReturn(documentAnalysis("ppt"));
        var upload = new MockMultipartFile("file", "aula.ppt", "application/vnd.ms-powerpoint", new byte[]{1, 2});
        mockMvc.perform(multipart("/v1/activities/from-ppt").file(upload).param("subject", "Ciências").param("grade", "4º Ano"))
                .andExpect(status().isOk());
    }

    @Test
    void generatesActivityFromValidLegacyPps() throws Exception {
        String text = "conteúdo pedagógico ".repeat(12);
        when(extractor.extract(any(), any())).thenReturn(new PptExtractionResult("aula.pps", 1, text, List.of(), true));
        when(documentAnalysisProvider.analyze(any())).thenReturn(documentAnalysis("pps"));
        var upload = new MockMultipartFile("file", "AULA.PPS", "application/octet-stream", new byte[]{1, 2});
        mockMvc.perform(multipart("/v1/activities/from-ppt").file(upload).param("subject", "Ciências").param("grade", "4º Ano"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsPptWithoutUsableText() throws Exception {
        when(extractor.extract(any(), any())).thenReturn(new PptExtractionResult("vazio.ppt", 1, "Slide 1", List.of(), false));
        var upload = new MockMultipartFile("file", "vazio.ppt", "application/vnd.ms-powerpoint", new byte[]{1});
        mockMvc.perform(multipart("/v1/activities/from-ppt").file(upload).param("subject", "Ciências").param("grade", "4º Ano"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("insufficient_ppt_text"));
    }

    @Test
    void rejectsInvalidPptInCompletePipeline() throws Exception {
        when(extractor.extract(any(), any())).thenThrow(new PptExtractionException("PPT inválido"));
        var upload = new MockMultipartFile("file", "invalido.ppt", "application/vnd.ms-powerpoint", new byte[]{1});
        mockMvc.perform(multipart("/v1/activities/from-ppt").file(upload).param("subject", "Ciências").param("grade", "4º Ano"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ppt_extraction_failed"));
    }

    private static String documentJson(String type) {
        return "{\"sourceType\":\"" + type + "\",\"sourceTitle\":\"Aula\",\"subject\":\"Ciências\",\"grade\":\"4º Ano\",\"text\":\""
                + "conteúdo pedagógico suficiente para analisar conceitos, relações, objetivos e evidências presentes somente no documento. ".repeat(2) + "\"}";
    }

    private static DocumentAnalysis documentAnalysis(String type) {
        var theme = new VideoAnalysis.Theme("Tema", List.of("Objetivo"), List.of("Conceito"), List.of("Relação"),
                List.of("Equívoco"), List.of(new VideoAnalysis.Evidence("Evidência", "document")));
        return new DocumentAnalysis(type, "Aula", "Ciências", "Resumo", List.of(theme), List.of());
    }
}
