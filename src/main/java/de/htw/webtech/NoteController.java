package de.htw.webtech;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.service.NoteService;
import de.htw.webtech.service.PdfService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    // Extracts the ID of the currently authenticated user from the JWT-backed SecurityContext
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AppUser user = (AppUser) auth.getPrincipal();
        return user.getId();
    }

    @Autowired
    private PdfService pdfService;

    @GetMapping
    public ResponseEntity<Iterable<Note>> getAllNotes() {
        return ResponseEntity.ok(service.getAll(getCurrentUserId()));
    }

    @GetMapping("/trash")
    public ResponseEntity<Iterable<Note>> getAllTrashedNotes() {
        return ResponseEntity.ok(service.getAllTrashed(getCurrentUserId()));
    }

    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllUniqueTags() {
        return ResponseEntity.ok(service.getAllUniqueTags(getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
        Note note = service.get(id, getCurrentUserId());
        return ResponseEntity.ok(note);
    }

    @PostMapping
    public ResponseEntity<Note> createNote(@Valid @RequestBody Note note) {
        Note createdNote = service.save(note, getCurrentUserId());
        return ResponseEntity.ok(createdNote);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id,
                                           @Valid @RequestBody Note note) {
        Note updatedNote = service.update(id, note, getCurrentUserId());
        return ResponseEntity.ok(updatedNote);
    }

    @GetMapping("/{id}/download/pdf")
    public ResponseEntity<byte[]> downloadNoteAsPdf(@PathVariable Long id) {
        Note note = service.get(id, getCurrentUserId());
        byte[] pdfBytes = pdfService.generatePdf(note);
        String filename = pdfService.sanitizeFilename(note.getTitle()) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    // Moving note to trash
    @PutMapping("/trash/{id}")
    public ResponseEntity<Note> moveToTrash(@PathVariable Long id) {
        Note trashedNote = service.moveToTrash(id, getCurrentUserId());
        return ResponseEntity.ok(trashedNote);
    }

    @PutMapping("/restore/{id}")
    public ResponseEntity<Note> restoreFromTrash(@PathVariable Long id) {
        Note restoredNote = service.restoreFromTrash(id, getCurrentUserId());
        return ResponseEntity.ok(restoredNote);
    }

    // Permanent delete
    @DeleteMapping("/permanent/{id}")
    public ResponseEntity<Void> deleteNotePermanently(@PathVariable Long id) {
        service.deletePermanently(id, getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
