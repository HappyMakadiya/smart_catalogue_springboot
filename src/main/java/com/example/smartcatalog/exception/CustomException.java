package com.example.smartcatalog.exception;

import org.springframework.http.HttpStatus;

/**
 * Abstract base class for all domain-specific exceptions.
 *
 * <p>Each subclass defines its own {@link HttpStatus} so the
 * {@link GlobalExceptionHandler} can map it to the correct RFC 7807
 * {@code ProblemDetail} without a separate {@code @ExceptionHandler}
 * per type.
 */
public abstract class CustomException extends RuntimeException {

    protected CustomException(String message) {
        super(message);
    }

    protected CustomException(String message, Throwable cause) {
        super(message, cause);
    }

    /** HTTP status code to use in the {@code ProblemDetail} response. */
    public abstract HttpStatus getStatus();
}
