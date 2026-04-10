package de.htw.webtech.security;

import de.htw.webtech.domain.AppUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // 32+ characters so HS256 / HMAC-SHA256 accepts it
    private static final String TEST_SECRET = "test-secret-at-least-32-bytes-long-123!";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(service, "expirationMs", ONE_HOUR_MS);
    }

    private AppUser user(String email) {
        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPassword("hashed");
        return u;
    }

    @Test
    void shouldGenerateAndExtractUsername() {
        AppUser user = user("alice@test.com");

        String token = service.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("alice@test.com", service.extractUsername(token));
    }

    @Test
    void shouldValidateTokenForCorrectUser() {
        AppUser user = user("alice@test.com");
        String token = service.generateToken(user);

        assertTrue(service.isTokenValid(token, user));
    }

    @Test
    void shouldRejectTokenForWrongSubject() {
        AppUser alice = user("alice@test.com");
        AppUser bob = user("bob@test.com");

        String token = service.generateToken(alice);

        assertFalse(service.isTokenValid(token, bob),
                "A token issued for alice must not be valid for bob");
    }

    @Test
    void shouldRejectExpiredToken() {
        // Issue a token that expired in the past (expiration = 1 ms)
        ReflectionTestUtils.setField(service, "expirationMs", 1L);
        AppUser user = user("alice@test.com");
        String token = service.generateToken(user);

        // Wait until the token is definitely expired
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Parsing an expired token throws ExpiredJwtException
        assertThrows(ExpiredJwtException.class, () -> service.extractUsername(token));
        assertThrows(ExpiredJwtException.class, () -> service.isTokenValid(token, user));
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        AppUser user = user("alice@test.com");

        // Build a service with a different secret, issue a token, then try to
        // parse it with the original service.
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret",
                "different-secret-also-at-least-32-bytes!");
        ReflectionTestUtils.setField(otherService, "expirationMs", ONE_HOUR_MS);
        String foreignToken = otherService.generateToken(user);

        assertThrows(SignatureException.class,
                () -> service.extractUsername(foreignToken));
    }

    @Test
    void shouldRejectTamperedToken() {
        AppUser user = user("alice@test.com");
        String token = service.generateToken(user);

        // Flip the last character of the signature portion
        char last = token.charAt(token.length() - 1);
        char replacement = last == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThrows(Exception.class,
                () -> service.extractUsername(tampered),
                "Tampering with the signature must cause parsing to fail");
    }
}
