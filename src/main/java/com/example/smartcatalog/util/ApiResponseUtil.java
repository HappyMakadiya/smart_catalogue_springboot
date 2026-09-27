package com.example.smartcatalog.util;

import com.example.smartcatalog.dto.ApiError;
import com.example.smartcatalog.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Factory helpers that build a {@link ResponseEntity} wrapping an {@link ApiResponse}.
 *
 * <p>Keeps controller and exception-handler code concise:
 * <pre>{@code
 * // Controller success example
 * return ApiResponseUtil.success("User registered successfully", tokenPayload, HttpStatus.CREATED);
 *
 * // Exception handler error example
 * return ApiResponseUtil.error("Not found", "NOT_FOUND", null, HttpStatus.NOT_FOUND);
 * }</pre>
 */
public final class ApiResponseUtil {

    private ApiResponseUtil() { /* utility class – no instances */ }

    // -----------------------------------------------------------------------
    // Success helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a success response with a custom HTTP status.
     *
     * @param message human-readable success summary
     * @param body    the actual payload (can be {@code null} for 204-style responses)
     * @param status  HTTP status to use (e.g. {@code HttpStatus.CREATED})
     * @param <T>     payload type
     * @return wrapped {@link ResponseEntity}
     */
    public static <T> ResponseEntity<ApiResponse<T>> success(
            String message,
            T body,
            HttpStatus status) {

        ApiResponse<T> response = ApiResponse.<T>builder()
                .statusCode(status.value())   // e.g. 200, 201
                .message(message)
                .error(null)
                .body(body)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Convenience overload that defaults to {@code 200 OK}.
     *
     * @param message human-readable success summary
     * @param body    the actual payload
     * @param <T>     payload type
     * @return {@code 200 OK} wrapped {@link ResponseEntity}
     */
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T body) {
        return success(message, body, HttpStatus.OK);
    }

    // -----------------------------------------------------------------------
    // Error helpers
    // -----------------------------------------------------------------------

    /**
     * Builds an error response with optional field-level validation details.
     *
     * @param message human-readable error summary
     * @param code    machine-readable error code (e.g. {@code "VALIDATION_ERROR"})
     * @param details field-level errors map — {@code null} when not applicable
     * @param status  HTTP status to use (e.g. {@code HttpStatus.BAD_REQUEST})
     * @return wrapped {@link ResponseEntity}
     */
    public static ResponseEntity<ApiResponse<Void>> error(
            String message,
            String code,
            Map<String, String> details,
            HttpStatus status) {

        ApiError apiError = ApiError.builder()
                .code(code)
                .details(details)
                .build();

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .statusCode(status.value())   // e.g. 400, 404, 409, 500
                .message(message)
                .error(apiError)
                .body(null)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    /**
     * Convenience overload when there are no field-level details.
     *
     * @param message human-readable error summary
     * @param code    machine-readable error code
     * @param status  HTTP status
     * @return wrapped {@link ResponseEntity}
     */
    public static ResponseEntity<ApiResponse<Void>> error(
            String message,
            String code,
            HttpStatus status) {

        return error(message, code, null, status);
    }
}
