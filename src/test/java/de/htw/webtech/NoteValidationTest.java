package de.htw.webtech;

import de.htw.webtech.controller.NoteController;
import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.security.JwtService;
import de.htw.webtech.service.NoteService;
import de.htw.webtech.service.PdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    @MockitoBean
    private PdfService pdfService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private AppUser mockUser() {
        AppUser appUser = new AppUser();
        appUser.setId(1L);
        appUser.setEmail("test@test.com");
        appUser.setPassword("hashed");
        return appUser;
    }

    @Test
    void shouldRejectNoteWithBlankTitle() throws Exception {
        mockMvc.perform(post("/api/notes")
                .with(user(mockUser())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"\", \"content\": \"Some content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNoteWithNullTitle() throws Exception {
        mockMvc.perform(post("/api/notes")
                .with(user(mockUser())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\": \"Some content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAcceptNoteWithValidTitle() throws Exception {
        Note note = new Note();
        note.setTitle("Valid Title");
        note.setContent("Valid Content");

        when(service.save(any(Note.class), anyLong())).thenReturn(note);

        mockMvc.perform(post("/api/notes")
                .with(user(mockUser())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"Valid Title\", \"content\": \"Valid Content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Valid Title"));
    }

    @Test
    void shouldRejectUpdateWithBlankTitle() throws Exception {
        mockMvc.perform(put("/api/notes/1")
                .with(user(mockUser())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"\", \"content\": \"Updated content\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNoteWithOnlyWhitespaceTitle() throws Exception {
        mockMvc.perform(post("/api/notes")
                .with(user(mockUser())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"   \", \"content\": \"Some content\"}"))
                .andExpect(status().isBadRequest());
    }
}
