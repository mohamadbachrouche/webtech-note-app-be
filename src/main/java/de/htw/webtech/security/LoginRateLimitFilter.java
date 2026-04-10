package de.htw.webtech.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP sliding-window rate limiter for {@code POST /api/auth/login}.
 *
 * Spec: max 10 attempts per IP per 15 minutes.
 *
 * Implementation notes:
 *   - In-memory only. Good enough for a single-instance deployment; a
 *     multi-instance deployment would need a shared store (Redis, etc.).
 *   - Uses a sliding window: per IP we keep a deque of attempt timestamps
 *     and prune anything older than the window on each request. This is
 *     more accurate than a fixed-window counter at the cost of a little
 *     memory per active IP.
 *   - Resolves the client IP with {@code X-Forwarded-For} precedence so
 *     the limiter works behind a reverse proxy (e.g. Render, nginx).
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    public static final int MAX_ATTEMPTS = 10;
    public static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Deque<Long>> attemptsByIp =
            new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only gate the login endpoint. Registration is not rate-limited here
        // (the spec only requires rate limiting /auth/login).
        // Use getRequestURI() rather than getServletPath() because the latter
        // is environment-dependent (empty on some MockMvc/servlet mappings).
        String uri = request.getRequestURI();
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = resolveClientIp(request);
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW.toMillis();

        Deque<Long> attempts = attemptsByIp.computeIfAbsent(
                ip, k -> new ArrayDeque<>());

        synchronized (attempts) {
            // Drop anything older than the sliding window.
            while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) {
                attempts.pollFirst();
            }

            if (attempts.size() >= MAX_ATTEMPTS) {
                writeTooManyRequests(response);
                return;
            }

            // Record this attempt BEFORE forwarding. This counts every
            // request — success or failure — against the limit, which
            // protects against credential stuffing where each attempt
            // is a different password.
            attempts.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    /** Extract the caller's IP, honouring X-Forwarded-For for proxied setups. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // XFF can be a comma-separated chain; the left-most is the original client.
            int comma = forwarded.indexOf(',');
            return (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
        Map<String, Object> body = Map.of(
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "message", "Too many login attempts. Try again later.",
                "timestamp", LocalDateTime.now().toString());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Visible for tests — lets test code reset the in-memory state between
     * cases so one test doesn't poison another. Public because integration
     * tests live in a sibling package.
     */
    public void resetForTests() {
        attemptsByIp.clear();
    }
}
