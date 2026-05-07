package com.lk.jtt808.device.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 保护 device 内部接口，避免未授权调用直接下发终端命令。
 */
@Component
public class InternalApiAuthFilter extends OncePerRequestFilter {

    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    @Value("${jtt808.internal-api.auth.enabled:true}")
    private boolean authEnabled;

    @Value("${jtt808.internal-api.auth.header-name:X-Internal-Token}")
    private String headerName;

    @Value("${jtt808.internal-api.auth.token:dev-internal-token}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!authEnabled || !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (expectedToken == null || expectedToken.isBlank()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Internal API token is not configured");
            return;
        }

        String actualToken = request.getHeader(headerName);
        if (!tokenMatches(expectedToken, actualToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal API token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean tokenMatches(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
