package de.htw.webtech.repository;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class NoteRepositoryTest {

    @Autowired
    private NoteRepository repository;

    @Autowired
    private UserRepository userRepository;

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
        Iterable<Note> result = repository.findAllByUserAndInTrashFalse(testUser);

        // 3. Assert: We should only find the 1 active note
        List<Note> notes = (List<Note>) result;
        assertEquals(1, notes.size());
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
        Iterable<Note> result = repository.findAllByUserAndInTrashTrue(testUser);

        // 3. Assert: We should only find the 1 trashed note
        List<Note> notes = (List<Note>) result;
        assertEquals(1, notes.size());
    }
}
