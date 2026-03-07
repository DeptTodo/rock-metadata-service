package com.rock.metadata.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * API Key authentication filter for MCP endpoints.
 * Validates the X-API-Key header or api_key query parameter.
 * Disabled when metadata.mcp.api-key is not configured.
 */
@Slf4j
@Component
@Order(1)
public class McpApiKeyFilter implements Filter {

    @Value("${metadata.mcp.api-key:}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        // Only protect MCP endpoints
        if (!path.startsWith("/mcp")) {
            chain.doFilter(request, response);
            return;
        }

        // If no API key configured, skip authentication
        if (apiKey == null || apiKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // Check X-API-Key header first, then query parameter
        String providedKey = httpReq.getHeader("X-API-Key");
        if (providedKey == null || providedKey.isBlank()) {
            providedKey = httpReq.getParameter("api_key");
        }

        if (!constantTimeEquals(apiKey, providedKey)) {
            log.warn("MCP authentication failed from {}", httpReq.getRemoteAddr());
            HttpServletResponse httpResp = (HttpServletResponse) response;
            httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResp.setContentType("application/json");
            httpResp.getWriter().write("{\"error\":\"Invalid or missing API key\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        if (provided == null) return false;
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
