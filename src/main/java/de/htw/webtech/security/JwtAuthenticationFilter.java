package de.htw.webtech.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.htw.webtech.domain.AppUser;
import de.htw.webtech.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Validates the {@code Authorization: Bearer <token>} header on every request
 * except the public auth endpoints ({@code /api/auth/register},
 * {@code /api/auth/login}).
 *
 * On success: loads the {@link AppUser} referenced by the token's subject
 * claim and places it in the {@link SecurityContextHolder} as the request
 * principal.
 *
 * On failure (missing header on a protected route, bad signature, expired
 * token, unknown user): writes a JSON 401 response and halts the filter
 * chain — this satisfies the "On failure: returns 401" requirement from
 * the spec.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Public endpoints: registration + login bypass JWT entirely.
        // Using getRequestURI() (not getServletPath()) because the latter
        // is empty on some MockMvc/servlet mappings, which would cause this
        // check to silently fail.
        return "/api/auth/register".equals(uri)
                || "/api/auth/login".equals(uri)
                // Allow H2 console in local dev without a token.
                || uri.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No credential presented on a protected route — fall through to
            // Spring Security, which will invoke the 401 entry point.
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        final Long userId;
        try {
            userId = jwtService.extractUserId(jwt);
        } catch (Exception e) {
            writeUnauthorized(response, "Invalid or expired token");
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            AppUser user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                writeUnauthorized(response, "Token references unknown user");
                return;
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "message", message,
                "timestamp", LocalDateTime.now().toString());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
