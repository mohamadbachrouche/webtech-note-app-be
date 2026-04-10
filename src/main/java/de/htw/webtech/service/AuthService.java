package de.htw.webtech.service;

import de.htw.webtech.domain.AppUser;
import de.htw.webtech.dto.AuthResponse;
import de.htw.webtech.dto.LoginRequest;
import de.htw.webtech.dto.RegisterRequest;
import de.htw.webtech.exception.EmailAlreadyUsedException;
import de.htw.webtech.repository.UserRepository;
import de.htw.webtech.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyUsedException(request.getEmail());
        }

        AppUser user = new AppUser();
        user.setEmail(request.getEmail());
        // BCrypt hash the password before saving — never store plain text.
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // save() returns the persisted entity with the generated ID populated,
        // which the JWT needs as its subject claim.
        AppUser saved = userRepository.save(user);

        String token = jwtService.generateToken(saved);
        return new AuthResponse(token, saved.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException on bad password (→ 401 via the
        // GlobalExceptionHandler). We also translate "user not found" into a
        // BadCredentialsException so the API never leaks which emails are
        // registered.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail());
    }
}
