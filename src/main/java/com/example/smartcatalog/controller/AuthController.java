package com.example.smartcatalog.controller;

import com.example.smartcatalog.dto.AuthResponse;
import com.example.smartcatalog.dto.LoginRequest;
import com.example.smartcatalog.dto.RegisterRequest;
import com.example.smartcatalog.model.User;
import com.example.smartcatalog.repository.UserRepository;
import com.example.smartcatalog.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication endpoints — no JWT required.
 *
 * <p>Demonstrates the full flow:
 * <ol>
 *   <li>{@code POST /auth/register} — creates a new user in the DB
 *       (password stored as BCrypt hash) and returns a JWT.</li>
 *   <li>{@code POST /auth/login} — authenticates existing credentials via
 *       {@link AuthenticationManager} (which internally calls
 *       {@link UserDetailsService#loadUserByUsername}) and returns a JWT.</li>
 * </ol>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------

    /**
     * Registers a new user.
     *
     * <p>Flow:
     * <ol>
     *   <li>Check if the username is already taken.</li>
     *   <li>Encode the password with BCrypt.</li>
     *   <li>Persist the {@link User} entity.</li>
     *   <li>Generate a JWT for immediate use (auto-login after register).</li>
     * </ol>
     *
     * @param request the registration payload (username + password)
     * @return 201 with JWT, or 409 if the username is taken
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        // Duplicate-username guard
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(AuthResponse.builder()
                            .message("Username '" + request.getUsername() + "' is already taken")
                            .build());
        }

        // Build and persist the user
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);

        // Generate JWT so the client is logged-in immediately
        String token = jwtUtil.generateToken(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AuthResponse.builder()
                        .token(token)
                        .message("User registered successfully")
                        .build());
    }

    // -------------------------------------------------------------------------
    // POST /auth/login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user and returns a JWT.
     *
     * <p>Internally delegates to {@link AuthenticationManager#authenticate},
     * which invokes the {@link com.example.smartcatalog.service.CustomUserDetailsService}
     * to load the user from the database and compares the BCrypt-encoded password.
     *
     * @param request the login payload (username + password)
     * @return 200 with JWT, or 401 if credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // This call triggers CustomUserDetailsService.loadUserByUsername()
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message("Invalid username or password")
                            .build());
        }

        // Authentication passed — load UserDetails and generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .token(token)
                        .message("Login successful")
                        .build()
        );
    }
}
