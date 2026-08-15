package com.estudenoah.backend.video;

import java.net.URI;

public interface VideoAnalysisService {
    VideoAnalysis analyze(URI youtubeUrl);
}
