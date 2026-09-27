package com.example.smartcatalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * Unified API response envelope used for <em>every</em> endpoint in the application.
 *
 * <h3>Shape</h3>
 * <pre>{@code
 * // Success (201 Created)
 * {
 *   "status_code": 201,
 *   "message":     "User registered successfully",
 *   "error":       null,
 *   "body":        { ...actual data... }
 * }
 *
 * // Failure (400 Bad Request)
 * {
 *   "status_code": 400,
 *   "message":     "Validation failed for one or more fields",
 *   "error":       { "code": "VALIDATION_ERROR", "details": { "username": "must not be blank" }, "timestamp": "..." },
 *   "body":        null
 * }
 * }</pre>
 *
 * <ul>
 *   <li>On <strong>success</strong>: {@code error} is {@code null}; {@code body} carries the payload.</li>
 *   <li>On <strong>failure</strong>: {@code body} is {@code null}; {@code error} carries the {@link ApiError}.</li>
 * </ul>
 *
 * @param <T> type of the success body payload
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)   // always serialize null fields so the shape is predictable
public class ApiResponse<T> {

    /**
     * HTTP status code of the response (e.g. 200, 201, 400, 404, 500).
     * Mirrors the HTTP response status so clients have it in the body too.
     */
    @JsonProperty("status_code")
    private int statusCode;

    /** Human-readable summary of the outcome (both success and error cases). */
    private String message;

    /**
     * Populated on failure; {@code null} on success.
     *
     * @see ApiError
     */
    private ApiError error;

    /**
     * Populated on success; {@code null} on failure.
     */
    private T body;
}
