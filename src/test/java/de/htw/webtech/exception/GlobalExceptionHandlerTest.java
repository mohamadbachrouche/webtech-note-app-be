package de.htw.webtech.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void noteNotFoundShouldReturn404WithMessage() {
        NoteNotFoundException ex = new NoteNotFoundException(42L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleNoteNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("42"));
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void emailAlreadyUsedShouldReturn409() {
        EmailAlreadyUsedException ex = new EmailAlreadyUsedException("taken@test.com");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleEmailAlreadyUsed(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("taken@test.com"));
    }

    @Test
    void badCredentialsShouldReturn401WithGenericMessage() {
        // Spring passes the raw message — handler must NOT forward it.
        BadCredentialsException ex = new BadCredentialsException("User alice@test.com not found");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBadCredentials(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid email or password", response.getBody().getMessage(),
                "Must not leak the raw BadCredentialsException message (may contain email)");
        assertFalse(response.getBody().getMessage().contains("alice@test.com"));
    }

    @Test
    void genericHandlerShouldNotLeakExceptionMessage() {
        String sensitiveMessage = "JDBC: connection string postgres://user:secret@db:5432/app failed";
        Exception ex = new RuntimeException(sensitiveMessage);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGlobalException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());

        String body = response.getBody().getMessage();
        assertFalse(body.contains("secret"),
                "Generic handler must not leak sensitive exception detail");
        assertFalse(body.contains("JDBC"),
                "Generic handler must not leak raw exception message");
        assertFalse(body.contains("postgres"),
                "Generic handler must not leak infrastructure detail");
        assertTrue(body.startsWith("An unexpected error occurred"),
                "Generic handler should return a user-facing placeholder");
    }

    @Test
    void genericHandlerShouldReturnUniqueCorrelationIds() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> first =
                handler.handleGlobalException(new RuntimeException("boom"));
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> second =
                handler.handleGlobalException(new RuntimeException("boom"));

        assertNotNull(first.getBody());
        assertNotNull(second.getBody());
        assertNotEquals(first.getBody().getMessage(), second.getBody().getMessage(),
                "Each 500 response should carry its own correlation ID");
    }
}
