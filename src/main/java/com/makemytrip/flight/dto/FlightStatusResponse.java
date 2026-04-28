package com.makemytrip.flight.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.makemytrip.flight.enums.DelayReason;
import com.makemytrip.flight.enums.FlightStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API response payload for a single flight's live status.
 *
 * All fields are read-only from the client's perspective.
 * Dates are serialised as ISO-8601 strings for easy JS parsing.
 *
 * Example JSON:
 * <pre>
 * {
 *   "flightNumber": "AI302",
 *   "airline": "Air India",
 *   "origin": "DEL",
 *   "destination": "BOM",
 *   "status": "DELAYED",
 *   "delayMinutes": 45,
 *   "delayReason": "WEATHER",
 *   "delayReasonDescription": "Adverse weather conditions",
 *   "scheduledDeparture": "2025-06-15T08:00:00",
 *   "estimatedDeparture": "2025-06-15T08:45:00",
 *   "estimatedArrival":   "2025-06-15T10:15:00",
 *   "gate": "B14",
 *   "terminal": "Terminal 2",
 *   "aircraftType": "Boeing 737-800",
 *   "lastUpdated": "2025-06-15T07:42:30",
 *   "minutesToDeparture": 37
 * }
 * </pre>
 */
@Data
@Builder
public class FlightStatusResponse {

    private String flightNumber;
    private String airline;
    private String origin;
    private String destination;

    /** Live operational status. */
    private FlightStatus status;

    /** Accumulated delay in minutes (0 when ON_TIME). */
    private int delayMinutes;

    /** Machine-readable delay reason enum. */
    private DelayReason delayReason;

    /** Human-readable description of the delay reason. */
    private String delayReasonDescription;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedArrival;

    private String gate;
    private String terminal;
    private String aircraftType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUpdated;

    /**
     * Countdown to departure in minutes.
     * Negative value means the flight has already departed.
     * Computed by the service layer, not stored in the DB.
     */
    private long minutesToDeparture;
}
