package de.htw.webtech;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class NoteController {

    @GetMapping("/api/notes")
    public List<Note> getAllNotes() {
        return List.of(
                new Note(1L, "My First Note", "This is the content.", LocalDateTime.now()),
                new Note(2L, "Shopping List", "Milk, Bread, Eggs", LocalDateTime.now()),
                new Note(3L, "Webtech M1", "Complete the GET route.", LocalDateTime.now())
        );
    }
}