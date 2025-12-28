package de.htw.webtech;

import de.htw.webtech.domain.Note;
import de.htw.webtech.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
class NoteValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService service;

    @Test
    void shouldRejectNoteWithBlankTitle() throws Exception {
        // Attempt to create a note with blank title
        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"\", \"content\": \"Some content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNoteWithNullTitle() throws Exception {
        // Attempt to create a note without title field
        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Some content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptNoteWithValidTitle() throws Exception {
        Note note = new Note();
        note.setTitle("Valid Title");
        note.setContent("Valid Content");

        when(service.save(any(Note.class))).thenReturn(note);

        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Valid Title\", \"content\": \"Valid Content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Valid Title"));
    }

    @Test
    void shouldRejectUpdateWithBlankTitle() throws Exception {
        // Attempt to update a note with blank title
        mockMvc.perform(put("/api/notes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"\", \"content\": \"Updated content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNoteWithOnlyWhitespaceTitle() throws Exception {
        // Attempt to create a note with only whitespace in title
        mockMvc.perform(post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"   \", \"content\": \"Some content\"}"))
                .andExpect(status().isBadRequest());
    }
}
