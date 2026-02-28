package de.htw.webtech;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.domain.Note;
import de.htw.webtech.exception.NoteNotFoundException;
import de.htw.webtech.security.JwtService;
import de.htw.webtech.service.NoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
class NoteErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NoteService service;

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
    void shouldReturn404WhenNoteNotFound() throws Exception {
        Long nonExistentId = 99999L;

        when(service.get(eq(nonExistentId), anyLong()))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(get("/api/notes/" + nonExistentId).with(user(mockUser())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Note with ID 99999 not found"));
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentNote() throws Exception {
        Long nonExistentId = 99999L;

        when(service.update(eq(nonExistentId), any(Note.class), anyLong()))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(put("/api/notes/" + nonExistentId)
                .with(user(mockUser())).with(csrf())
                .contentType("application/json")
                .content("{\"title\": \"Test\", \"content\": \"Content\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn404WhenMovingNonExistentNoteToTrash() throws Exception {
        Long nonExistentId = 99999L;

        when(service.moveToTrash(eq(nonExistentId), anyLong()))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(put("/api/notes/trash/" + nonExistentId).with(user(mockUser())).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn404WhenRestoringNonExistentNote() throws Exception {
        Long nonExistentId = 99999L;

        when(service.restoreFromTrash(eq(nonExistentId), anyLong()))
                .thenThrow(new NoteNotFoundException(nonExistentId));

        mockMvc.perform(put("/api/notes/restore/" + nonExistentId).with(user(mockUser())).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
