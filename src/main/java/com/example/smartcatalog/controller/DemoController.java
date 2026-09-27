package com.example.smartcatalog.controller;

import com.example.smartcatalog.dto.ApiResponse;
import com.example.smartcatalog.util.ApiResponseUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Protected endpoints that require a valid JWT in the {@code Authorization} header.
 *
 * <p>These endpoints demonstrate that:
 * <ul>
 *   <li>The {@link com.example.smartcatalog.security.JwtAuthenticationFilter} intercepts the
 *       request, extracts the token, and populates the {@code SecurityContext}.</li>
 *   <li>The {@link com.example.smartcatalog.service.CustomUserDetailsService} loads the user
 *       from the database during token validation.</li>
 *   <li>{@link AuthenticationPrincipal} injects the authenticated {@link UserDetails}
 *       directly into the controller method.</li>
 * </ul>
 *
 * <p>Try calling these <strong>without</strong> a token — you'll get a {@code 403 Forbidden}.
 * Include {@code Authorization: Bearer <token>} and you'll get back your profile data.
 *
 * <p>All responses use the unified {@link ApiResponse} envelope.
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    /**
     * Returns the authenticated user's profile information.
     *
     * <p>Proves the full chain works:
     * <br>{@code JWT → JwtAuthenticationFilter → CustomUserDetailsService → SecurityContext → @AuthenticationPrincipal}
     *
     * @param userDetails the authenticated principal injected by Spring Security
     * @return username and authorities extracted from the token-backed UserDetails
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> profile = Map.of(
                "username",    userDetails.getUsername(),
                "authorities", userDetails.getAuthorities(),
                "info",        "Data loaded by CustomUserDetailsService."
        );

        return ApiResponseUtil.ok("Profile fetched successfully", profile);
    }

    /**
     * Simple hello endpoint for a quick smoke test.
     *
     * @param userDetails the authenticated principal
     * @return a greeting with the username
     */
    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<Map<String, String>>> hello(
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, String> greeting = Map.of(
                "greeting", "Hello, " + userDetails.getUsername() + "! 🚀",
                "info",     "If you can see this, your JWT is valid and the security chain is working."
        );

        return ApiResponseUtil.ok("Hello fetched successfully", greeting);
    }
}
