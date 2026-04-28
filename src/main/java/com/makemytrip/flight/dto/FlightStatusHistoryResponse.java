package com.makemytrip.flight.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.makemytrip.flight.enums.DelayReason;
import com.makemytrip.flight.enums.FlightStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API response payload for a single flight status history entry.
 * Used to render the timeline in the frontend.
 */
@Data
@Builder
public class FlightStatusHistoryResponse {

    private String flightNumber;
    private FlightStatus status;
    private int delayMinutes;
    private DelayReason delayReason;
    private String changeDescription;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedArrival;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime recordedAt;
}
