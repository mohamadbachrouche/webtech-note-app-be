package de.htw.webtech.security;

import de.htw.webtech.domain.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Issues and validates HS256 JWTs for authenticated users.
 *
 * Token payload shape (per auth spec):
 *   {
 *     "sub":   <userId as string>,
 *     "email": <user email>,
 *     "iat":   <issued-at epoch seconds>,
 *     "exp":   <expiry epoch seconds>
 *   }
 *
 * The signing key is derived from the {@code app.jwt.secret} property, which
 * MUST be provided via the {@code JWT_SECRET} environment variable. Tokens
 * default to a 7 day lifetime ({@code app.jwt.expiration-ms}).
 */
@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";

    @Value("${app.jwt.secret}")
    private String secret;

    // 7 days in milliseconds — matches the authentication spec default.
    @Value("${app.jwt.expiration-ms:604800000}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        // HS256 requires at least a 256-bit (32 byte) key.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT whose subject is the user ID and which also
     * carries the user's email as a custom claim.
     */
    public String generateToken(AppUser user) {
        if (user.getId() == null) {
            throw new IllegalStateException(
                    "Cannot issue JWT for unsaved user (id is null)");
        }
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claims(Map.of(CLAIM_EMAIL, user.getEmail()))
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses and verifies signature + expiry. Any failure (bad signature,
     * tampering, expired, malformed) results in an exception the caller
     * should treat as an authentication failure.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        String sub = parseAndValidate(token).getSubject();
        try {
            return Long.parseLong(sub);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("JWT subject is not a numeric user id", e);
        }
    }

    public String extractEmail(String token) {
        Object email = parseAndValidate(token).get(CLAIM_EMAIL);
        return email == null ? null : email.toString();
    }
}
