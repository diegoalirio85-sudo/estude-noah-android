package com.estudenoah.backend.api;

import com.estudenoah.backend.activity.ActivityGenerationRequest;
import com.estudenoah.backend.activity.ActivityGenerationService;
import com.estudenoah.backend.activity.GeneratedActivity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class ActivityController {
    private final ActivityGenerationService service;

    public ActivityController(ActivityGenerationService service) {
        this.service = service;
    }

    @PostMapping(path = "/v1/activities/generate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public GeneratedActivity generate(@RequestBody(required = false) ActivityGenerationRequest request) {
        return service.generate(request);
    }
}
