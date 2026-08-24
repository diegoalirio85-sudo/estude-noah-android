package com.estudenoah.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public final class PayloadLimitFilter extends OncePerRequestFilter {
    private final long maxJsonBytes;
    private final long maxUploadBytes;

    public PayloadLimitFilter(@Value("${security.payload.max-json-bytes:1048576}") long maxJsonBytes,
                              @Value("${security.payload.max-upload-bytes:53477376}") long maxUploadBytes) {
        this.maxJsonBytes = maxJsonBytes;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        String contentType = request.getContentType();
        long limit = contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/")
                ? maxUploadBytes : maxJsonBytes;
        if (contentLength > limit) {
            response.setStatus(413);
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"payload_too_large\",\"message\":\"Request payload exceeds the configured limit.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

