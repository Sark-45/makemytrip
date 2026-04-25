package com.makemytrip.flight.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for {@code DELETE /api/flights/untrack}.
 */
@Data
public class UntrackFlightRequest {

    @NotBlank(message = "userId must not be blank")
    private String userId;

    @NotBlank(message = "flightNumber must not be blank")
    private String flightNumber;
}
