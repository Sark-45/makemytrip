package com.makemytrip.flight.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a tracking business rule is violated, e.g.
 * attempting to track the same flight twice.  Maps to HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class TrackingException extends RuntimeException {

    public TrackingException(String message) {
        super(message);
    }
}
