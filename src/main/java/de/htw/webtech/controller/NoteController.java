package de.htw.webtech.controller;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.dto.NoteCreateRequest;
import de.htw.webtech.dto.NoteResponse;
import de.htw.webtech.dto.NoteUpdateRequest;
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

import java.util.ArrayList;
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

    private static List<NoteResponse> toResponseList(Iterable<Note> notes) {
        List<NoteResponse> out = new ArrayList<>();
        for (Note n : notes) {
            out.add(NoteResponse.from(n));
        }
        return out;
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getAllNotes() {
        return ResponseEntity.ok(toResponseList(service.getAll(getCurrentUserId())));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<NoteResponse>> getAllTrashedNotes() {
        return ResponseEntity.ok(toResponseList(service.getAllTrashed(getCurrentUserId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNoteById(@PathVariable Long id) {
        Note note = service.get(id, getCurrentUserId());
        return ResponseEntity.ok(NoteResponse.from(note));
    }

    @PostMapping
    public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody NoteCreateRequest request) {
        Note createdNote = service.create(request, getCurrentUserId());
        return ResponseEntity.ok(NoteResponse.from(createdNote));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(@PathVariable Long id,
                                                   @Valid @RequestBody NoteUpdateRequest request) {
        Note updatedNote = service.update(id, request, getCurrentUserId());
        return ResponseEntity.ok(NoteResponse.from(updatedNote));
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
    public ResponseEntity<NoteResponse> moveToTrash(@PathVariable Long id) {
        Note trashedNote = service.moveToTrash(id, getCurrentUserId());
        return ResponseEntity.ok(NoteResponse.from(trashedNote));
    }

    @PutMapping("/restore/{id}")
    public ResponseEntity<NoteResponse> restoreFromTrash(@PathVariable Long id) {
        Note restoredNote = service.restoreFromTrash(id, getCurrentUserId());
        return ResponseEntity.ok(NoteResponse.from(restoredNote));
    }

    // Permanent delete
    @DeleteMapping("/permanent/{id}")
    public ResponseEntity<Void> deleteNotePermanently(@PathVariable Long id) {
        service.deletePermanently(id, getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
