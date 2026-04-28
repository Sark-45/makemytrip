package com.makemytrip.flight.exception;

import com.makemytrip.flight.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception handler for all flight-related REST controllers.
 *
 * Converts exceptions into consistent {@link ApiResponse} envelopes so
 * clients never receive raw stack traces or Spring's default error JSON.
 *
 * Every handler logs the error at an appropriate level:
 * - Business exceptions (4xx) → WARN
 * - Unexpected exceptions (5xx) → ERROR
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 404 – Flight not found in live data engine.
     */
    @ExceptionHandler(FlightNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleFlightNotFound(
            FlightNotFoundException ex) {
        log.warn("FlightNotFoundException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 409 – Tracking business rule violation (e.g., duplicate tracking).
     */
    @ExceptionHandler(TrackingException.class)
    public ResponseEntity<ApiResponse<Void>> handleTrackingException(
            TrackingException ex) {
        log.warn("TrackingException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * 400 – @Valid / @Validated bean validation failure.
     *
     * Collects all field errors into a single human-readable message,
     * e.g. "flightNumber: must match IATA pattern; userId: must not be blank"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation error: " + errors));
    }

    /**
     * 500 – Unexpected runtime exception catch-all.
     *
     * Logs the full stack trace at ERROR level but returns only a
     * generic message to the client (avoids leaking internals).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. Please try again later."));
    }
}
