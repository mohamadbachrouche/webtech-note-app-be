package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.repository.NoteRepository;
import de.htw.webtech.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository repository;

    @Mock
    private UserRepository userRepository;

    private NoteService service;

    private static final Long USER_ID = 1L;
    private AppUser testUser;

    @BeforeEach
    void setUp() {
        service = new NoteService(repository, userRepository);

        testUser = new AppUser();
        testUser.setId(USER_ID);
        testUser.setEmail("test@test.com");
        testUser.setPassword("hashed");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
    }

    @Test
    void shouldSaveNote() {
        Note note = new Note();
        note.setTitle("Test Note");

        when(repository.save(note)).thenReturn(note);

        Note savedNote = service.save(note, USER_ID);

        assertEquals("Test Note", savedNote.getTitle());
        assertEquals(testUser, savedNote.getUser());
        verify(repository).save(note);
    }

    @Test
    void shouldMoveToTrash() {
        Long id = 1L;
        Note note = new Note();
        note.setId(id);
        note.setUser(testUser);
        note.setPinned(true);
        note.setInTrash(false);

        when(repository.findByIdAndUser(id, testUser)).thenReturn(Optional.of(note));
        when(repository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note trashedNote = service.moveToTrash(id, USER_ID);

        assertTrue(trashedNote.isInTrash());
        assertFalse(trashedNote.isPinned());
        verify(repository).save(note);
    }

    @Test
    void shouldReturnAllUniqueTagsSortedAndDeduplicated() {
        Note note1 = new Note();
        note1.setTitle("Note 1");
        note1.setTags("work,important");
        note1.setUser(testUser);

        Note note2 = new Note();
        note2.setTitle("Note 2");
        note2.setTags("personal, work, urgent");
        note2.setUser(testUser);

        Note note3 = new Note();
        note3.setTitle("Note 3");
        note3.setTags("");
        note3.setUser(testUser);

        when(repository.findAllByUserAndInTrashFalse(testUser)).thenReturn(List.of(note1, note2, note3));

        List<String> tags = service.getAllUniqueTags(USER_ID);

        assertEquals(List.of("important", "personal", "urgent", "work"), tags);
    }

    @Test
    void shouldSaveNoteWithTags() {
        Note note = new Note();
        note.setTitle("Tagged Note");
        note.setTags("work,important");

        when(repository.save(note)).thenReturn(note);

        Note saved = service.save(note, USER_ID);

        assertEquals("work,important", saved.getTags());
        verify(repository).save(note);
    }

    @Test
    void shouldDefaultNullTagsToEmptyOnSave() {
        Note note = new Note();
        note.setTitle("No Tags");
        note.setTags(null);

        when(repository.save(note)).thenReturn(note);

        service.save(note, USER_ID);

        assertEquals("", note.getTags());
    }

    @Test
    void shouldRestoreFromTrash() {
        Long id = 1L;
        Note note = new Note();
        note.setId(id);
        note.setUser(testUser);
        note.setInTrash(true);

        when(repository.findByIdAndUser(id, testUser)).thenReturn(Optional.of(note));
        when(repository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note restoredNote = service.restoreFromTrash(id, USER_ID);

        assertFalse(restoredNote.isInTrash());
        verify(repository).save(note);
    }
}
