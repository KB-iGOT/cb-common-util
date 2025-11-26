package org.igot.common;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Custom exception class for handling application-specific errors with HTTP status codes.
 * <p>
 * This exception extends {@link RuntimeException} and provides additional context
 * including an error code, custom message, and associated HTTP status code.
 * It is designed to be used in Spring applications for consistent error handling
 * across different layers of the application.
 * </p>
 *
 * @author Karthikeyan R (karthik-tarento)
 */
@Getter
@Setter
@Component
public class CustomException extends RuntimeException {
    /**
     * Error code identifying the specific type of error.
     */
    private String code;

    /**
     * Detailed error message describing what went wrong.
     */
    private String message;

    /**
     * HTTP status code to be returned in the response.
     */
    private HttpStatus httpStatusCode;

    /**
     * Default no-argument constructor.
     * Creates a new instance of CustomException with no initial values.
     */
    public CustomException() {
    }

    /**
     * Constructs a new CustomException with the specified error details.
     *
     * @param code the error code identifying the type of error
     * @param message the detailed error message
     * @param httpStatusCode the HTTP status code to be returned
     */
    public CustomException(String code, String message, HttpStatus httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}
