package de.htw.webtech;

import de.htw.webtech.domain.Note;
import de.htw.webtech.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5177",
        "https://webtech-note-app-fe.onrender.com"
})
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
        return note != null ? ResponseEntity.ok(note) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Note> createNote(@RequestBody Note note) {
        Note createdNote = service.save(note);
        return ResponseEntity.ok(createdNote);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody Note note) {
        Note updatedNote = service.update(id, note);
        return updatedNote != null ? ResponseEntity.ok(updatedNote) : ResponseEntity.notFound().build();
    }

    // --- MODIFIED: This is now for MOVING to trash ---
    @PutMapping("/trash/{id}")
    public ResponseEntity<Note> moveToTrash(@PathVariable Long id) {
        Note trashedNote = service.moveToTrash(id);
        return trashedNote != null ? ResponseEntity.ok(trashedNote) : ResponseEntity.notFound().build();
    }

    // --- NEW ---
    @PutMapping("/restore/{id}")
    public ResponseEntity<Note> restoreFromTrash(@PathVariable Long id) {
        Note restoredNote = service.restoreFromTrash(id);
        return restoredNote != null ? ResponseEntity.ok(restoredNote) : ResponseEntity.notFound().build();
    }

    // --- NEW: This is for PERMANENT delete ---
    @DeleteMapping("/permanent/{id}")
    public ResponseEntity<Void> deleteNotePermanently(@PathVariable Long id) {
        service.deletePermanently(id);
        return ResponseEntity.ok().build();
    }
}