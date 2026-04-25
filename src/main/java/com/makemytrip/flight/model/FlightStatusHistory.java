package com.makemytrip.flight.model;

import com.makemytrip.flight.enums.DelayReason;
import com.makemytrip.flight.enums.FlightStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Immutable audit record written every time a flight's status changes.
 *
 * Enables "status history" feature: clients can request a timeline of
 * every state transition a flight has gone through during the day.
 *
 * Collection: {@code flight_status_history}
 */
@Document(collection = "flight_status_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightStatusHistory {

    @Id
    private String id;

    /** The flight this history entry belongs to. */
    @Indexed
    private String flightNumber;

    /** Status recorded at the time of this entry. */
    private FlightStatus status;

    /** Delay in minutes at the time of this entry (0 if ON_TIME). */
    private int delayMinutes;

    /** Reason for delay at the time of this entry. */
    private DelayReason delayReason;

    /** ETA captured at the time of this entry. */
    private LocalDateTime estimatedArrival;

    /** Exact wall-clock time this status was recorded. */
    private LocalDateTime recordedAt;

    /** Free-text message describing what changed, for display on timeline. */
    private String changeDescription;
}
