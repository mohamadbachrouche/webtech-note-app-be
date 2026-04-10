package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.dto.AuthResponse;
import de.htw.webtech.dto.LoginRequest;
import de.htw.webtech.dto.RegisterRequest;
import de.htw.webtech.exception.EmailAlreadyUsedException;
import de.htw.webtech.repository.UserRepository;
import de.htw.webtech.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
        when(jwtService.generateToken(any(AppUser.class))).thenReturn("jwt-token");

        AuthResponse response = service.register(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("new@test.com", response.getEmail());

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        assertEquals("new@test.com", savedUser.getEmail());
        assertEquals("hashed-pw", savedUser.getPassword(),
                "Password must be encoded, never stored in plain text");
    }

    @Test
    void shouldThrowWhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("taken@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        EmailAlreadyUsedException ex = assertThrows(
                EmailAlreadyUsedException.class,
                () -> service.register(request));
        assertTrue(ex.getMessage().contains("taken@test.com"));

        verify(userRepository, never()).save(any(AppUser.class));
        verify(passwordEncoder, never()).encode(anyString());
        verify(jwtService, never()).generateToken(any(AppUser.class));
    }

    @Test
    void shouldLoginWithValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        AppUser user = new AppUser();
        user.setEmail("user@test.com");
        user.setPassword("hashed-pw");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = service.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("user@test.com", response.getEmail());

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        assertEquals("user@test.com", tokenCaptor.getValue().getPrincipal());
        assertEquals("password123", tokenCaptor.getValue().getCredentials());
    }

    @Test
    void shouldThrowOnBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> service.login(request));

        // Must not reach token generation
        verify(jwtService, never()).generateToken(any(AppUser.class));
        verify(userRepository, never()).findByEmail(anyString());
    }
}
