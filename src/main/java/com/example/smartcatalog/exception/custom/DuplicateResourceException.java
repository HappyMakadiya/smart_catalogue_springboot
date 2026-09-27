package com.example.smartcatalog.exception.custom;

import com.example.smartcatalog.exception.CustomException;
import com.example.smartcatalog.exception.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a create/update would violate a uniqueness constraint
 * (e.g. duplicate username).
 *
 * <p>Mapped to {@code 409 Conflict} via {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends CustomException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String field, Object value) {
        super(resourceName + " with " + field + " '" + value + "' already exists");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
