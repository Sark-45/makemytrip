package com.makemytrip.flight.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Tracks which flights a user is watching.
 *
 * A compound unique index on (userId, flightNumber) prevents
 * duplicate tracking entries for the same user/flight pair.
 *
 * Collection: {@code user_flight_tracking}
 */
@Document(collection = "user_flight_tracking")
@CompoundIndexes({
    @CompoundIndex(name = "user_flight_idx",
                   def = "{'userId': 1, 'flightNumber': 1}",
                   unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFlightTracking {

    @Id
    private String id;

    /** The user who added this tracking entry (maps to your User entity). */
    private String userId;

    /** IATA flight number being tracked, e.g. "AI302". */
    private String flightNumber;

    /** When the user started tracking this flight. */
    private LocalDateTime trackedAt;
}
