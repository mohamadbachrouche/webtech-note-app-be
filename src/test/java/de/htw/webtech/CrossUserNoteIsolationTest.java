package de.htw.webtech;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.htw.webtech.security.LoginRateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test for per-user note isolation (Phase 2 / Phase 3 spec item #11):
 *
 *   "a note created by user A cannot be read or modified by user B."
 *
 * Uses the real Spring Security filter chain, real JwtService, real AuthService
 * and a real in-memory H2 database — the only stubs are the login rate limiter
 * being reset between test methods so earlier runs don't poison later ones.
 *
 * Flow per test:
 *   1. Register two distinct users via /api/auth/register, capturing each JWT.
 *   2. User A creates a note.
 *   3. Assert that user B:
 *        - cannot GET /api/notes/{id}    → 403
 *        - cannot PUT /api/notes/{id}    → 403
 *        - cannot DELETE /api/notes/permanent/{id} → 403
 *        - does not see the note in their own GET /api/notes list
 *   4. Assert user A *can* still operate on their own note.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CrossUserNoteIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginRateLimitFilter loginRateLimitFilter;

    @BeforeEach
    void resetRateLimiter() {
        // Integration tests share JVM state across runs within a suite —
        // wipe the limiter so a previous test can't exhaust the budget.
        loginRateLimitFilter.resetForTests();
    }

    /** Registers a user and returns their JWT. */
    private String register(String email, String password) throws Exception {
        String json = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());
        // Spec shape: AuthResponse includes `token`.
        return body.get("token").asText();
    }

    /** Creates a note as the given user and returns its id. */
    private long createNote(String token, String title) throws Exception {
        String payload = String.format(
                "{\"title\":\"%s\",\"content\":\"hello\"}", title);
        MvcResult result = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(
                result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    @Test
    void userBCannotReadOrModifyUserANote() throws Exception {
        // NOTE: unique emails per test to avoid conflicts on repeated runs.
        String tokenA = register("alice-" + System.nanoTime() + "@example.com", "alicepass1");
        String tokenB = register("bob-" + System.nanoTime() + "@example.com",   "bobpass456");

        long noteId = createNote(tokenA, "Alice's private note");

        // ---- User B is forbidden from reading Alice's note ----
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // ---- User B is forbidden from updating Alice's note ----
        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"hijacked\",\"content\":\"pwned\"}"))
                .andExpect(status().isForbidden());

        // ---- User B is forbidden from trashing Alice's note ----
        mockMvc.perform(put("/api/notes/trash/" + noteId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // ---- User B is forbidden from permanently deleting Alice's note ----
        mockMvc.perform(delete("/api/notes/permanent/" + noteId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // ---- User B's note list does NOT include Alice's note ----
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                // Bob has no notes of his own, so the list is empty.
                .andExpect(jsonPath("$.length()").value(0));

        // ---- User A can still read their own note ----
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Alice's private note"));
    }

    @Test
    void requestsWithoutTokenAreRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestsWithInvalidTokenAreRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerReturnsConflictOnDuplicateEmail() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";
        register(email, "strongpass1");

        String json = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}", email, "strongpass1");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        String json = "{\"email\":\"short@example.com\",\"password\":\"1234567\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturns401OnWrongPassword() throws Exception {
        String email = "wrongpw-" + System.nanoTime() + "@example.com";
        register(email, "correcthorse1");

        String json = String.format(
                "{\"email\":\"%s\",\"password\":\"notright99\"}", email);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRateLimiterBlocksAfter10Attempts() throws Exception {
        // Limiter is reset in @BeforeEach; spam 10 failed logins, the 11th
        // must be rejected with 429 regardless of what the credentials are.
        String json = "{\"email\":\"nobody@example.com\",\"password\":\"badpass11\"}";
        for (int i = 0; i < LoginRateLimitFilter.MAX_ATTEMPTS; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void loginResponseNeverContainsPassword() throws Exception {
        String email = "nopw-" + System.nanoTime() + "@example.com";
        register(email, "securepw1");

        String json = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\"}", email, "securepw1");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // Defensive: the response must never carry the password hash or
        // plaintext in any casing.
        org.junit.jupiter.api.Assertions.assertFalse(
                body.toLowerCase().contains("password"),
                "login response should not include the password field: " + body);
    }
}
