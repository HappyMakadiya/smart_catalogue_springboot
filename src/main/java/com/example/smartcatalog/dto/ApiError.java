package com.example.smartcatalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Structured error payload embedded inside {@link ApiResponse} when a request fails.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code code}      — machine-readable error code (e.g. {@code "VALIDATION_ERROR"}).</li>
 *   <li>{@code details}   — optional field-level validation errors (field → message).</li>
 *   <li>{@code timestamp} — when the error occurred (UTC).</li>
 * </ul>
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /** Machine-readable error code (e.g. {@code "NOT_FOUND"}, {@code "VALIDATION_ERROR"}). */
    private String code;

    /**
     * Optional map of field-level validation errors.
     * Present only for {@code 400 Bad Request} / validation failures.
     */
    private Map<String, String> details;

    /** UTC instant at which this error was generated. */
    @Builder.Default
    private Instant timestamp = Instant.now();
}
