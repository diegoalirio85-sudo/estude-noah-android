package com.estudenoah.backend.security;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryRequestRateLimiter {
    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final Clock clock;

    public InMemoryRequestRateLimiter(int requestsPerMinute) {
        this(requestsPerMinute, Clock.systemUTC());
    }

    InMemoryRequestRateLimiter(int requestsPerMinute, Clock clock) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.clock = clock;
    }

    public boolean allow(String clientKey) {
        long minute = clock.millis() / 60_000;
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(entry -> entry.getValue().minute < minute);
        }
        Window window = windows.compute(clientKey, (key, current) -> {
            if (current == null || current.minute != minute) return new Window(minute, 1);
            return new Window(minute, current.count + 1);
        });
        return window.count <= requestsPerMinute;
    }

    private record Window(long minute, int count) {
    }
}

