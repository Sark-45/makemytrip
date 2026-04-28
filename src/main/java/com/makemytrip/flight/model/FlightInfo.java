package com.makemytrip.flight.model;

import com.makemytrip.flight.enums.DelayReason;
import com.makemytrip.flight.enums.FlightStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a single flight's live operational data.
 *
 * This document is stored in MongoDB and also mirrored in the
 * in-memory {@code ConcurrentHashMap} inside MockFlightDataEngine
 * for fast reads by the scheduler.  MongoDB is the durable store;
 * the map is the hot cache updated every tick.
 *
 * Collection: {@code flights}
 */
@Document(collection = "flights")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FlightInfo {

    @Id
    private String id;

    /**
     * IATA flight number, e.g. "AI302", "6E4521".
     * Indexed for O(1) look-ups from the REST layer.
     */
    @Indexed(unique = true)
    private String flightNumber;

    /** Airline operating the flight, e.g. "Air India". */
    private String airline;

    /** IATA origin airport code, e.g. "DEL". */
    private String origin;

    /** IATA destination airport code, e.g. "BOM". */
    private String destination;

    /** Scheduled departure (immutable once seeded). */
    private LocalDateTime scheduledDeparture;

    /**
     * Effective departure time.  Starts equal to scheduledDeparture;
     * incremented by the simulator whenever a delay is applied.
     */
    private LocalDateTime estimatedDeparture;

    /**
     * Dynamically recalculated arrival time.
     * = scheduledArrival + total delay accumulated so far.
     */
    private LocalDateTime estimatedArrival;

    /** Total accumulated delay in minutes (0 when ON_TIME). */
    private int delayMinutes;

    /** Current operational state of the flight. */
    private FlightStatus status;

    /** Reason for the delay; NONE when status is not DELAYED. */
    private DelayReason delayReason;

    /**
     * Wall-clock time of the last simulator tick that changed this record.
     * Sent to clients so they can detect stale data.
     */
    private LocalDateTime lastUpdated;

    /** Gate number, e.g. "B14". Populated by the simulator. */
    private String gate;

    /** Terminal, e.g. "Terminal 2". */
    private String terminal;

    /** Aircraft type, e.g. "Boeing 737-800". */
    private String aircraftType;
}
