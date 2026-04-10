package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.dto.NoteCreateRequest;
import de.htw.webtech.repository.NoteRepository;
import de.htw.webtech.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    }

    @Test
    void shouldCreateNoteFromRequest() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

        NoteCreateRequest request = new NoteCreateRequest();
        request.setTitle("Test Note");
        request.setContent("Body");
        request.setTags("tag1,tag2");
        request.setColor("#33aaff");
        request.setPinned(true);

        when(repository.save(any(Note.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Note savedNote = service.create(request, USER_ID);

        assertEquals("Test Note", savedNote.getTitle());
        assertEquals("Body", savedNote.getContent());
        assertEquals("tag1,tag2", savedNote.getTags());
        assertEquals("#33aaff", savedNote.getColor());
        assertTrue(savedNote.isPinned());
        assertFalse(savedNote.isInTrash());
        assertEquals(testUser, savedNote.getUser());
        verify(repository).save(any(Note.class));
    }

    @Test
    void shouldMoveToTrash() {
        Long id = 1L;
        Note note = new Note();
        note.setId(id);
        note.setUser(testUser);
        note.setPinned(true);
        note.setInTrash(false);

        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(note));
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

        when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(Optional.of(note));
        when(repository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note restoredNote = service.restoreFromTrash(id, USER_ID);

        assertFalse(restoredNote.isInTrash());
        verify(repository).save(note);
    }
}
