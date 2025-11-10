package de.htw.webtech.service;

import de.htw.webtech.domain.Note;
import de.htw.webtech.repository.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime; // Import this

@Service
public class NoteService {

    @Autowired
    private NoteRepository repository;

    public Note save(Note note) {
        // Set creation/modification times
        if (note.getId() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }
        note.setLastModified(LocalDateTime.now());
        return repository.save(note);
    }

    public Note get(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Iterable<Note> getAll() {
        return repository.findAll();
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

    public void delete(Long id) {
        repository.deleteById(id);
    }
}