package de.htw.webtech;

import de.htw.webtech.domain.Note;
import de.htw.webtech.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService service;

    @Test
    void shouldReturnAllNotes() throws Exception {
        Note note1 = new Note();
        note1.setTitle("Note 1");
        note1.setInTrash(false);

        Note note2 = new Note();
        note2.setTitle("Note 2");
        note2.setInTrash(false);

        when(service.getAll()).thenReturn(List.of(note1, note2));

        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Note 1"));
    }

    @Test
    void shouldReturnNoteById() throws Exception {
        Note note = new Note();
        note.setTitle("My Note");

        when(service.get(42L)).thenReturn(note);

        mockMvc.perform(get("/api/notes/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Note"));
    }

    @Test
    void shouldCreateNote() throws Exception {
        Note note = new Note();
        note.setTitle("New Note");

        when(service.save(any(Note.class))).thenReturn(note);

        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"New Note\", \"content\": \"Content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Note"));
    }

    @Test
    void shouldUpdateNote() throws Exception {
        Note updatedNote = new Note();
        updatedNote.setId(1L);
        updatedNote.setTitle("Updated Title");

        // When service.update is called with ID 1 and ANY note object, return updatedNote
        when(service.update(eq(1L), any(Note.class))).thenReturn(updatedNote);

        mockMvc.perform(put("/api/notes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Updated Title\", \"content\": \"New content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void shouldMoveToTrash() throws Exception {
        Note trashNote = new Note();
        trashNote.setId(1L);
        trashNote.setInTrash(true);

        when(service.moveToTrash(1L)).thenReturn(trashNote);

        mockMvc.perform(put("/api/notes/trash/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inTrash").value(true));
    }

    @Test
    void shouldDeletePermanently() throws Exception {
        // Since delete is void, we just check status 200
        mockMvc.perform(delete("/api/notes/permanent/1"))
                .andExpect(status().isOk());

        verify(service).deletePermanently(1L);
    }
}