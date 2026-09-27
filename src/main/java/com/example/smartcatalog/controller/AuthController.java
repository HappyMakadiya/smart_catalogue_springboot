package com.example.smartcatalog.controller;

import com.example.smartcatalog.dto.ApiResponse;
import com.example.smartcatalog.dto.AuthResponse;
import com.example.smartcatalog.dto.LoginRequest;
import com.example.smartcatalog.dto.RegisterRequest;
import com.example.smartcatalog.exception.custom.AuthenticationFailedException;
import com.example.smartcatalog.exception.custom.DuplicateResourceException;
import com.example.smartcatalog.model.User;
import com.example.smartcatalog.repository.UserRepository;
import com.example.smartcatalog.security.JwtUtil;
import com.example.smartcatalog.util.ApiResponseUtil;
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
 * <p>All responses follow the unified {@link ApiResponse} envelope:
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
     * @return 201 with JWT inside {@link ApiResponse}, or 409 if the username is taken
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        // Duplicate-username guard
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
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

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .message("User registered successfully")
                .build();

        return ApiResponseUtil.success("User registered successfully", authResponse, HttpStatus.CREATED);
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
     * @return 200 with JWT inside {@link ApiResponse}, or 401 if credentials are invalid
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            // This call triggers CustomUserDetailsService.loadUserByUsername()
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        // Authentication passed — load UserDetails and generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .message("Login successful")
                .build();

        return ApiResponseUtil.ok("Login successful", authResponse);
    }
}
