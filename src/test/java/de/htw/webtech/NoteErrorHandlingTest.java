package de.htw.webtech;

import de.htw.webtech.domain.Note;
import de.htw.webtech.exception.NoteNotFoundException;
import de.htw.webtech.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
class NoteErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService service;

    @Test
    void shouldReturn404WhenNoteNotFound() throws Exception {
        Long nonExistentId = 99999L;

        when(service.get(nonExistentId))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(get("/api/notes/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Note with ID 99999 not found"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentNote() throws Exception {
        Long nonExistentId = 99999L;

        when(service.update(eq(nonExistentId), any(Note.class)))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(put("/api/notes/" + nonExistentId)
                .contentType("application/json")
                .content("{\"title\": \"Test\", \"content\": \"Content\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn404WhenMovingNonExistentNoteToTrash() throws Exception {
        Long nonExistentId = 99999L;

        when(service.moveToTrash(nonExistentId))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(put("/api/notes/trash/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn404WhenRestoringNonExistentNote() throws Exception {
        Long nonExistentId = 99999L;

        when(service.restoreFromTrash(nonExistentId))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(put("/api/notes/restore/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
