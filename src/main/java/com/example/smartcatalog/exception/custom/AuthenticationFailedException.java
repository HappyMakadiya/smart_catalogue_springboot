package com.example.smartcatalog.exception.custom;

import com.example.smartcatalog.exception.CustomException;
import com.example.smartcatalog.exception.GlobalExceptionHandler;
import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication fails (e.g. invalid username or password).
 *
 * <p>Mapped to {@code 401 Unauthorized} via {@link GlobalExceptionHandler}.
 */
public class AuthenticationFailedException extends CustomException {

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
