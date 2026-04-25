package com.makemytrip.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for {@code POST /api/flights/track}.
 *
 * Bean-validation annotations ensure callers cannot submit
 * blank or structurally invalid flight numbers.
 */
@Data
public class TrackFlightRequest {

    /**
     * The user who wants to track this flight.
     * In a production system this would be resolved from the
     * JWT/session principal; accepting it in the request body
     * keeps the demo self-contained.
     */
    @NotBlank(message = "userId must not be blank")
    private String userId;

    /**
     * IATA flight number to track, e.g. "AI302", "6E4521".
     * Pattern allows 2–3 letter airline code + 1–4 digit flight number.
     */
    @NotBlank(message = "flightNumber must not be blank")
    @Pattern(
        regexp = "^[A-Z0-9]{2,3}[0-9]{1,4}$",
        message = "flightNumber must be a valid IATA number, e.g. AI302"
    )
    private String flightNumber;
}
