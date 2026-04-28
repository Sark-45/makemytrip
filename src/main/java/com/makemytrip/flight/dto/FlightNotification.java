package com.makemytrip.flight.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.makemytrip.flight.enums.FlightStatus;
import com.makemytrip.flight.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Payload pushed over STOMP WebSocket to topic
 * {@code /topic/flights/{flightNumber}} whenever a status change occurs.
 *
 * The frontend deserialises this JSON and updates the flight card
 * in real time without a page refresh.
 *
 * Example JSON:
 * <pre>
 * {
 *   "flightNumber": "AI302",
 *   "notificationType": "DELAY_UPDATE",
 *   "message": "AI302 is now delayed by 45 minutes due to Adverse weather conditions.",
 *   "newStatus": "DELAYED",
 *   "delayMinutes": 45,
 *   "newEstimatedArrival": "2025-06-15T10:15:00",
 *   "timestamp": "2025-06-15T07:42:30"
 * }
 * </pre>
 */
@Data
@Builder
public class FlightNotification {

    private String flightNumber;

    /** Category of the update, drives notification icon on frontend. */
    private NotificationType notificationType;

    /** Human-readable message suitable for display in a toast/popup. */
    private String message;

    /** The new status after this change. */
    private FlightStatus newStatus;

    /** New total delay in minutes (0 if recovered to ON_TIME). */
    private int delayMinutes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime newEstimatedDeparture;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime newEstimatedArrival;

    /** Server wall-clock time when the notification was generated. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
