package de.htw.webtech.repository;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class NoteRepositoryTest {

    @Autowired
    private NoteRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setEmail("test@test.com");
        testUser.setPassword("hashed");
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldFindActiveNotes() {
        // 1. Arrange: Save one active note and one trashed note
        Note activeNote = new Note();
        activeNote.setTitle("Active Note");
        activeNote.setInTrash(false);
        activeNote.setUser(testUser);
        repository.save(activeNote);

        Note trashNote = new Note();
        trashNote.setTitle("Trash Note");
        trashNote.setInTrash(true);
        trashNote.setUser(testUser);
        repository.save(trashNote);

        // 2. Act: Call the user-scoped query
        Iterable<Note> result = repository.findAllByUserIdAndInTrashFalse(testUser.getId());

        // 3. Assert: We should only find the 1 active note
        List<Note> notes = (List<Note>) result;
        assertEquals(1, notes.size());
    }

    @Test
    void shouldSetTimestampsOnCreate() {
        Note note = new Note();
        note.setTitle("Timestamp Test");
        note.setUser(testUser);

        Note saved = repository.save(note);
        entityManager.flush();

        assertNotNull(saved.getCreatedAt(), "createdAt should be set on create");
        assertNotNull(saved.getLastModified(), "lastModified should be set on create");
    }

    @Test
    void shouldUpdateLastModifiedAfterUpdate() throws InterruptedException {
        // Create and persist a note
        Note note = new Note();
        note.setTitle("Original Title");
        note.setUser(testUser);
        note = repository.save(note);
        entityManager.flush();
        entityManager.clear();

        Note saved = repository.findById(note.getId()).orElseThrow();
        LocalDateTime originalCreatedAt = saved.getCreatedAt();
        LocalDateTime originalLastModified = saved.getLastModified();

        // Small delay to ensure timestamp differs
        Thread.sleep(50);

        // Update the note
        saved.setTitle("Updated Title");
        repository.save(saved);
        entityManager.flush();
        entityManager.clear();

        Note updated = repository.findById(note.getId()).orElseThrow();

        assertEquals(originalCreatedAt, updated.getCreatedAt(),
                "createdAt should not change after update");
        assertTrue(updated.getLastModified().isAfter(originalLastModified),
                "lastModified should be later than original after update");
    }

    @Test
    void shouldFindTrashedNotes() {
        // 1. Arrange
        Note activeNote = new Note();
        activeNote.setTitle("Active Note");
        activeNote.setInTrash(false);
        activeNote.setUser(testUser);
        repository.save(activeNote);

        Note trashNote = new Note();
        trashNote.setTitle("Trash Note");
        trashNote.setInTrash(true);
        trashNote.setUser(testUser);
        repository.save(trashNote);

        // 2. Act
        Iterable<Note> result = repository.findAllByUserIdAndInTrashTrue(testUser.getId());

        // 3. Assert: We should only find the 1 trashed note
        List<Note> notes = (List<Note>) result;
        assertEquals(1, notes.size());
    }

    @Test
    void shouldPersistColorAcrossCreateUpdateAndFetch() {
        Note note = new Note();
        note.setTitle("Color Test");
        note.setColor("#22cc88");
        note.setUser(testUser);

        Note created = repository.save(note);
        entityManager.flush();
        entityManager.clear();

        Note afterCreate = repository.findByIdAndUserId(created.getId(), testUser.getId()).orElseThrow();
        assertEquals("#22cc88", afterCreate.getColor());

        afterCreate.setColor("#ff9900");
        repository.save(afterCreate);
        entityManager.flush();
        entityManager.clear();

        Note afterUpdate = repository.findByIdAndUserId(created.getId(), testUser.getId()).orElseThrow();
        assertEquals("#ff9900", afterUpdate.getColor());
    }
}
