package com.example.smartcatalog.exception.custom;

import com.example.smartcatalog.exception.CustomException;
import com.example.smartcatalog.exception.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist (e.g. user, catalog item).
 *
 * <p>Mapped to {@code 404 Not Found} via {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends CustomException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with identifier: " + identifier);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
