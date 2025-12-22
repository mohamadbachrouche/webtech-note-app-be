package de.htw.webtech.service;

import de.htw.webtech.domain.Note;
import de.htw.webtech.repository.NoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository repository;

    @InjectMocks
    private NoteService service;

    @Test
    void shouldSaveNote() {
        Note note = new Note();
        note.setTitle("Test Note");

        // We simulate the repository behavior
        when(repository.save(note)).thenReturn(note);

        Note savedNote = service.save(note);

        assertEquals("Test Note", savedNote.getTitle());
        verify(repository).save(note);
    }

    @Test
    void shouldMoveToTrash() {
        Long id = 1L;
        Note note = new Note();
        note.setId(id);
        note.setPinned(true);
        note.setInTrash(false);

        when(repository.findById(id)).thenReturn(Optional.of(note));
        // Return the note that is passed to save()
        when(repository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note trashedNote = service.moveToTrash(id);

        // Verify logic: should be in trash and unpinned
        assertTrue(trashedNote.isInTrash());
        assertFalse(trashedNote.isPinned());
        verify(repository).save(note);
    }

    @Test
    void shouldRestoreFromTrash() {
        Long id = 1L;
        Note note = new Note();
        note.setId(id);
        note.setInTrash(true);

        when(repository.findById(id)).thenReturn(Optional.of(note));
        when(repository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Note restoredNote = service.restoreFromTrash(id);

        assertFalse(restoredNote.isInTrash());
        verify(repository).save(note);
    }
}