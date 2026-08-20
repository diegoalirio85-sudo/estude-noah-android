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

@WebMvcTest({HealthController.class, MaterialController.class, YoutubeMaterialController.class, ActivityController.class,
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
}
