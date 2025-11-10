package de.htw.webtech.service;

import de.htw.webtech.domain.Note;
import de.htw.webtech.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class NoteService {

    @Autowired
    private NoteRepository repository;

    public Note save(Note note) {
        if (note.getId() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }
        note.setLastModified(LocalDateTime.now());
        return repository.save(note);
    }

    public Note get(Long id) {
        return repository.findById(id).orElse(null);
    }

    // --- MODIFIED ---
    // Only get notes that are NOT in the trash
    public Iterable<Note> getAll() {
        return repository.findAllByInTrashFalse();
    }

    // --- NEW ---
    // Get only notes that ARE in the trash
    public Iterable<Note> getAllTrashed() {
        return repository.findAllByInTrashTrue();
    }

    public Note update(Long id, Note updatedNote) {
        Note existingNote = repository.findById(id).orElse(null);
        if (existingNote != null) {
            existingNote.setTitle(updatedNote.getTitle());
            existingNote.setContent(updatedNote.getContent());
            existingNote.setTags(updatedNote.getTags());
            existingNote.setPinned(updatedNote.isPinned());
            existingNote.setInTrash(updatedNote.isInTrash());
            existingNote.setLastModified(LocalDateTime.now());
            return repository.save(existingNote);
        }
        return null;
    }

    // --- MODIFIED ---
    // This no longer permanently deletes. It just sets the flag.
    public Note moveToTrash(Long id) {
        Note note = repository.findById(id).orElse(null);
        if (note != null) {
            note.setInTrash(true);
            note.setPinned(false); // Can't be pinned and in trash
            note.setLastModified(LocalDateTime.now());
            return repository.save(note);
        }
        return null;
    }

    // --- NEW ---
    public Note restoreFromTrash(Long id) {
        Note note = repository.findById(id).orElse(null);
        if (note != null) {
            note.setInTrash(false);
            note.setLastModified(LocalDateTime.now());
            return repository.save(note);
        }
        return null;
    }

    // --- NEW (was the old delete) ---
    // This is now for permanent deletion
    public void deletePermanently(Long id) {
        repository.deleteById(id);
    }
}