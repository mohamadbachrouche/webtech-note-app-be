package de.htw.webtech;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Startup hook that protects against an edge case from the per-user notes
 * migration: a pre-existing database where the {@code notes.user_id}
 * column may be null (because it was seeded before user scoping existed).
 *
 * Behaviour:
 *   1. If there are any notes with {@code user_id IS NULL}, find-or-create
 *      a dedicated "legacy" default user and assign those rows to it.
 *   2. If all notes already have an owner, this runner is a no-op.
 *
 * Rationale:
 *   - The {@link de.htw.webtech.domain.Note} entity now declares the FK as
 *     {@code nullable = false}, so keeping orphans around would eventually
 *     blow up on subsequent saves or schema refreshes.
 *   - A fresh deployment has no notes at all, so this runner is silent.
 *
 * The default legacy user is created with a cryptographically random
 * password — nobody knows it, so it cannot be logged into. It exists
 * purely as a foreign-key target. Operators wanting to reclaim those notes
 * can reset the password manually.
 */
@Component
public class OrphanNoteMigrationRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(OrphanNoteMigrationRunner.class);

    private static final String LEGACY_EMAIL = "legacy-orphans@note-app.local";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public OrphanNoteMigrationRunner(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Quick head-check: does the notes table even have orphans?
        // Done via native SQL so we don't accidentally filter-on-null via JPQL.
        Number orphanCount;
        try {
            orphanCount = (Number) entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM note WHERE user_id IS NULL")
                    .getSingleResult();
        } catch (Exception e) {
            // Table may not exist yet on a brand-new DB before Hibernate runs DDL.
            // In that case there are obviously no orphans — bail out quietly.
            log.debug("Orphan note check skipped: {}", e.getMessage());
            return;
        }

        if (orphanCount == null || orphanCount.longValue() == 0) {
            return;
        }

        AppUser legacyUser = userRepository.findByEmail(LEGACY_EMAIL)
                .orElseGet(this::createLegacyUser);

        int updated = entityManager
                .createNativeQuery("UPDATE note SET user_id = :uid WHERE user_id IS NULL")
                .setParameter("uid", legacyUser.getId())
                .executeUpdate();

        log.warn("Reassigned {} orphan note(s) to legacy user id={} ({}).",
                updated, legacyUser.getId(), LEGACY_EMAIL);
    }

    private AppUser createLegacyUser() {
        AppUser user = new AppUser();
        user.setEmail(LEGACY_EMAIL);
        user.setPassword(passwordEncoder.encode(randomPassword()));
        AppUser saved = userRepository.save(user);
        log.warn("Created legacy owner user id={} ({}) for orphan notes.",
                saved.getId(), LEGACY_EMAIL);
        return saved;
    }

    private static String randomPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        // Prefix ensures the encoded result comfortably exceeds the 8-char
        // validation floor even before base64 expansion.
        return "legacy-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
