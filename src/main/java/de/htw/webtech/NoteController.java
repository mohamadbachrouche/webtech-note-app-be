package de.htw.webtech;

import de.htw.webtech.domain.Note;
import de.htw.webtech.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes") // --- ADDED: Base path for all note APIs
public class NoteController {

    @Autowired
    private NoteService service;

    @GetMapping
    public ResponseEntity<Iterable<Note>> getAllNotes() {
        return ResponseEntity.ok(service.getAll());
    }

    // --- NEW ---
    @GetMapping("/trash")
    public ResponseEntity<Iterable<Note>> getAllTrashedNotes() {
        return ResponseEntity.ok(service.getAllTrashed());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
        Note note = service.get(id);
        return ResponseEntity.ok(note);
    }

    @PostMapping
    public ResponseEntity<Note> createNote(@Valid @RequestBody Note note) {
        Note createdNote = service.save(note);
        return ResponseEntity.ok(createdNote);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @Valid @RequestBody Note note) {
        Note updatedNote = service.update(id, note);
        return ResponseEntity.ok(updatedNote);
    }

    // --- MODIFIED: This is now for MOVING to trash ---
    @PutMapping("/trash/{id}")
    public ResponseEntity<Note> moveToTrash(@PathVariable Long id) {
        Note trashedNote = service.moveToTrash(id);
        return ResponseEntity.ok(trashedNote);
    }

    // --- NEW ---
    @PutMapping("/restore/{id}")
    public ResponseEntity<Note> restoreFromTrash(@PathVariable Long id) {
        Note restoredNote = service.restoreFromTrash(id);
        return ResponseEntity.ok(restoredNote);
    }

    // --- NEW: This is for PERMANENT delete ---
    @DeleteMapping("/permanent/{id}")
    public ResponseEntity<Void> deleteNotePermanently(@PathVariable Long id) {
        service.deletePermanently(id);
        return ResponseEntity.ok().build();
    }
}