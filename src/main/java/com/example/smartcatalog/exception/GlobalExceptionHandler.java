package com.example.smartcatalog.exception;

import com.example.smartcatalog.dto.ApiResponse;
import com.example.smartcatalog.exception.custom.DuplicateResourceException;
import com.example.smartcatalog.exception.custom.ResourceNotFoundException;
import com.example.smartcatalog.util.ApiResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * Centralised error handling for the entire REST API.
 *
 * <p>Every error response follows the unified {@link ApiResponse} envelope:
 * <pre>{@code
 * {
 *   "status":  "ERROR",
 *   "message": "<human-readable summary>",
 *   "error":   { "code": "<MACHINE_CODE>", "details": { ... }, "timestamp": "..." },
 *   "body":    null
 * }
 * }</pre>
 *
 * <h3>Handled scenarios</h3>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} — Bean Validation failures
 *       (e.g. {@code @NotBlank}, {@code @Size}).</li>
 *   <li>{@link CustomException} subclasses — custom business-rule violations
 *       such as {@link ResourceNotFoundException} (404) and
 *       {@link DuplicateResourceException} (409).</li>
 *   <li>All Spring MVC exceptions (405, 404, 415, etc.) — intercepted via
 *       {@link #handleExceptionInternal} so every response uses the
 *       {@link ApiResponse} envelope.</li>
 *   <li>Any unhandled {@link Exception} — caught as a safety net and returned
 *       as {@code 500 Internal Server Error}.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------
    // 1. Bean Validation failures  (400 Bad Request)
    // ------------------------------------------------------------------

    /**
     * Overrides the default Spring handling to return a unified {@link ApiResponse}
     * containing a field → message {@code details} map inside the {@code error} object.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        // Collect every field error into a field → message map
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (msg1, msg2) -> msg1 + "; " + msg2   // merge duplicate field errors
                ));

        ResponseEntity<ApiResponse<Void>> errorResponse = ApiResponseUtil.error(
                "Validation failed for one or more fields",
                "VALIDATION_ERROR",
                fieldErrors,
                HttpStatus.BAD_REQUEST
        );

        // Cast to Object to satisfy the ResponseEntityExceptionHandler signature
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse.getBody());
    }

    // ------------------------------------------------------------------
    // 2. All other Spring MVC exceptions (405, 404, 415, 400, …)
    // ------------------------------------------------------------------

    /**
     * Single choke-point for every Spring MVC exception that
     * {@link ResponseEntityExceptionHandler} handles internally — e.g.:
     * <ul>
     *   <li>405 {@code HttpRequestMethodNotSupportedException}</li>
     *   <li>404 {@code NoHandlerFoundException} / {@code NoResourceFoundException}</li>
     *   <li>415 {@code HttpMediaTypeNotSupportedException}</li>
     *   <li>400 {@code HttpMessageNotReadableException}</li>
     *   <li>… and more</li>
     * </ul>
     *
     * <p>Instead of delegating to Spring's default RFC 7807 Problem Detail body,
     * we replace it with our own {@link ApiResponse} envelope so <em>every</em>
     * error response is shaped identically.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // Derive a clean machine-readable code from the HTTP status name
        // e.g. HttpStatus.METHOD_NOT_ALLOWED → "METHOD_NOT_ALLOWED"
        String code = httpStatus.name();

        // Use the exception message for well-known 4xx/5xx; fall back to reason phrase
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : httpStatus.getReasonPhrase();

        log.warn("Spring MVC exception [{}]: {}", code, message);

        ResponseEntity<ApiResponse<Void>> errorResponse = ApiResponseUtil.error(
                message,
                code,
                httpStatus
        );

        return ResponseEntity
                .status(httpStatus)
                .headers(headers)
                .body(errorResponse.getBody());
    }

    // ------------------------------------------------------------------
    // 3. Custom domain exceptions  (status varies per subclass)
    // ------------------------------------------------------------------

    /**
     * Catches any {@link CustomException} subclass and translates it into a unified
     * {@link ApiResponse} whose HTTP status comes from {@link CustomException#getStatus()}.
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException ex) {

        log.warn("Custom exception: {}", ex.getMessage());

        return ApiResponseUtil.error(
                ex.getMessage(),
                ex.getStatus().name(),          // e.g. "NOT_FOUND", "CONFLICT"
                ex.getStatus()
        );
    }

    // ------------------------------------------------------------------
    // 4. Catch-all for unexpected errors  (500 Internal Server Error)
    // ------------------------------------------------------------------

    /**
     * Safety net — anything not caught above results in a generic 500 response
     * that hides implementation details from the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {

        log.error("Unhandled exception", ex);

        return ApiResponseUtil.error(
                "An unexpected error occurred. Please try again later.",
                "INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
