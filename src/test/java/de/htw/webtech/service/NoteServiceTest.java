package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.exception.NoteNotFoundException;
import de.htw.webtech.repository.NoteRepository;
import de.htw.webtech.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository repository;

    @Mock
    private UserRepository userRepository;

    private NoteService service;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private AppUser testUser;
    private AppUser otherUser;

    @BeforeEach
    void setUp() {
        service = new NoteService(repository, userRepository);

        testUser = new AppUser();
        testUser.setId(USER_ID);
        testUser.setEmail("test@test.com");
        testUser.setPassword("hashed");

        otherUser = new AppUser();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setEmail("other@test.com");
        otherUser.setPassword("hashed");
    }

    @Test
    void shouldSaveNote() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
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

        when(repository.findById(id)).thenReturn(Optional.of(note));
        when(repository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note trashedNote = service.moveToTrash(id, USER_ID);

        assertTrue(trashedNote.isInTrash());
        assertFalse(trashedNote.isPinned());
        verify(repository).save(note);
    }

    @Test
    void shouldRestoreFromTrash() {
        Long id = 1L;
        Note note = new Note();
        note.setId(id);
        note.setUser(testUser);
        note.setInTrash(true);

        when(repository.findById(id)).thenReturn(Optional.of(note));
        when(repository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note restoredNote = service.restoreFromTrash(id, USER_ID);

        assertFalse(restoredNote.isInTrash());
        verify(repository).save(note);
    }

    @Test
    void shouldReturn404WhenNoteDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoteNotFoundException.class, () -> service.get(99L, USER_ID));
    }

    @Test
    void shouldReturn403WhenNoteBelongsToAnotherUser() {
        Long id = 5L;
        Note note = new Note();
        note.setId(id);
        note.setUser(otherUser); // owned by user 2

        when(repository.findById(id)).thenReturn(Optional.of(note));

        // user 1 tries to read user 2's note — must be denied, not 404'd.
        assertThrows(AccessDeniedException.class, () -> service.get(id, USER_ID));
    }

    @Test
    void shouldReturn403WhenUpdatingNoteOfAnotherUser() {
        Long id = 5L;
        Note note = new Note();
        note.setId(id);
        note.setUser(otherUser);

        when(repository.findById(id)).thenReturn(Optional.of(note));

        Note incoming = new Note();
        incoming.setTitle("Hijacked");
        assertThrows(AccessDeniedException.class,
                () -> service.update(id, incoming, USER_ID));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturn403WhenDeletingNoteOfAnotherUser() {
        Long id = 5L;
        Note note = new Note();
        note.setId(id);
        note.setUser(otherUser);

        when(repository.findById(id)).thenReturn(Optional.of(note));

        assertThrows(AccessDeniedException.class,
                () -> service.deletePermanently(id, USER_ID));
        verify(repository, never()).delete(any());
    }
}
