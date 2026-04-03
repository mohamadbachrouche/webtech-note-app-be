package de.htw.webtech;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.security.JwtService;
import de.htw.webtech.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService service;

    // Required by JwtAuthenticationFilter (loaded as a Filter bean in WebMvcTest)
    @MockitoBean
    private JwtService jwtService;

    // Required by SecurityConfig's DaoAuthenticationProvider
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
    void shouldReturnAllNotes() throws Exception {
        Note note1 = new Note();
        note1.setTitle("Note 1");
        note1.setInTrash(false);

        Note note2 = new Note();
        note2.setTitle("Note 2");
        note2.setInTrash(false);

        when(service.getAll(anyLong())).thenReturn(List.of(note1, note2));

        mockMvc.perform(get("/api/notes").with(user(mockUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Note 1"));
    }

    @Test
    void shouldReturnNoteById() throws Exception {
        Note note = new Note();
        note.setTitle("My Note");

        when(service.get(eq(42L), anyLong())).thenReturn(note);

        mockMvc.perform(get("/api/notes/42").with(user(mockUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Note"));
    }

    @Test
    void shouldCreateNote() throws Exception {
        Note note = new Note();
        note.setTitle("New Note");

        when(service.save(any(Note.class), anyLong())).thenReturn(note);

        mockMvc.perform(post("/api/notes")
                        .with(user(mockUser())).with(csrf())
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

        when(service.update(eq(1L), any(Note.class), anyLong())).thenReturn(updatedNote);

        mockMvc.perform(put("/api/notes/1")
                        .with(user(mockUser())).with(csrf())
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

        when(service.moveToTrash(eq(1L), anyLong())).thenReturn(trashNote);

        mockMvc.perform(put("/api/notes/trash/1").with(user(mockUser())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inTrash").value(true));
    }

    @Test
    void shouldDeletePermanently() throws Exception {
        mockMvc.perform(delete("/api/notes/permanent/1").with(user(mockUser())).with(csrf()))
                .andExpect(status().isOk());

        verify(service).deletePermanently(eq(1L), anyLong());
    }
}
