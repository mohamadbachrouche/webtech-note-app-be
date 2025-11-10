package de.htw.webtech;

import de.htw.webtech.domain.Note;
import de.htw.webtech.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Import this

@RestController
@CrossOrigin(origins = { // Make sure this is up-to-date
        "http://localhost:5173",
        "http://localhost:5177",
        "https://webtech-note-app-fe.onrender.com"
})
public class NoteController {

    @Autowired
    private NoteService service;

    @GetMapping("/api/notes")
    public ResponseEntity<Iterable<Note>> getAllNotes() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/api/notes/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
        Note note = service.get(id);
        return note != null ? ResponseEntity.ok(note) : ResponseEntity.notFound().build();
    }

    // This is the POST route for M4
    @PostMapping("/api/notes")
    public ResponseEntity<Note> createNote(@RequestBody Note note) {
        Note createdNote = service.save(note);
        return ResponseEntity.ok(createdNote);
    }

    @PutMapping("/api/notes/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody Note note) {
        Note updatedNote = service.update(id, note);
        return updatedNote != null ? ResponseEntity.ok(updatedNote) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/api/notes/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}