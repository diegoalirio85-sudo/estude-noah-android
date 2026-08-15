package com.estudenoah.backend.api;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp) {
}
