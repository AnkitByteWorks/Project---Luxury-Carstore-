package com.Luxurycars.carstore.config;

import com.Luxurycars.carstore.service.RateLimitService;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Order(1)   // ← runs BEFORE JwtAuthFilter
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = clientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        String category = categorize(uri, method);

        if (category == null) {
            // Not a limited endpoint
            filterChain.doFilter(request, response);
            return;
        }

        if (rateLimitService.tryConsume(ip, category)) {
            // OK — continue
            response.setHeader("X-RateLimit-Category", category);
            filterChain.doFilter(request, response);
        } else {
            // Limit exceeded — 429
            long retryAfter = rateLimitService.secondsUntilRefill(ip, category);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());  // 429
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            Map<String, Object> body = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded for " + category
                            + ". Try again in " + retryAfter + " seconds.",
                    "path", uri
            );

            objectMapper.writeValue(response.getOutputStream(), body);
        }
    }

    // ─── Determine category from URL + method ───
    private String categorize(String uri, String method) {
        if (uri.startsWith("/api/auth/")) return "auth";
        if (uri.matches("/api/cars/\\d+/image") && "POST".equals(method)) return "upload";
        if (uri.startsWith("/api/cars/") && !"GET".equals(method)) return "admin";
        if (uri.startsWith("/api/cars") || uri.startsWith("/api/orders")) return "public";
        return null;  // no limit (e.g., swagger, actuator)
    }

    // ─── Get client IP (handles proxies) ───
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}