package de.htw.webtech;

import de.htw.webtech.domain.Note; // Import your domain Note
import de.htw.webtech.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NoteController {

    @Autowired
    private NoteService service;

    @GetMapping("/api/notes")
    public ResponseEntity<Iterable<Note>> getAllNotes() {
        // This now fetches from the database via the service
        return ResponseEntity.ok(service.getAll());
    }
}