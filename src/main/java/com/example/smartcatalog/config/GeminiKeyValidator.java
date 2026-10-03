package com.example.smartcatalog.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates that a real Gemini API key has been supplied before the application
 * accepts any traffic.
 */
@Component
public class GeminiKeyValidator {

    @Value("${spring.ai.google.genai.embedding.api-key}")
    private String apiKey;

    @PostConstruct
    public void validate() {
        if (apiKey == null
                || apiKey.isBlank()
                || apiKey.equals("MISSING")
                || apiKey.startsWith("${")) {

            throw new IllegalStateException("""
                    ============================================================
                     GEMINI_API_KEY is not configured!
                    ============================================================
                     The environment variable GEMINI_API_KEY must be set before
                     starting the application.

                     Get your free API key at:
                     https://aistudio.google.com/app/apikey

                     Quick fix (terminal):
                       export GEMINI_API_KEY=AIza...
                       ./mvnw spring-boot:run

                     IntelliJ: Run → Edit Configurations → Environment variables
                       GEMINI_API_KEY=AIza...
                    ============================================================
                    """);
        }
    }
}
