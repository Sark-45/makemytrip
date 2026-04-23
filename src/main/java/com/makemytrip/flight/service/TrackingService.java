package com.makemytrip.flight.service;

import com.makemytrip.flight.dto.FlightStatusResponse;
import com.makemytrip.flight.engine.MockFlightDataEngine;
import com.makemytrip.flight.exception.FlightNotFoundException;
import com.makemytrip.flight.exception.TrackingException;
import com.makemytrip.flight.model.FlightInfo;
import com.makemytrip.flight.model.UserFlightTracking;
import com.makemytrip.flight.repository.UserFlightTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  TrackingService
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  Manages the "tracked flights" list for each user.
 *
 *  A user can track multiple flights simultaneously.  Each tracked flight is
 *  stored as a {@link UserFlightTracking} document in MongoDB; when the user
 *  fetches their list, live status is joined from the in-memory store
 *  managed by {@link MockFlightDataEngine}.
 *
 *  Business rules enforced here:
 *  ① A user cannot track the same flight twice.
 *  ② A user cannot track a flight that does not exist in the data engine.
 *  ③ Untracking a flight that is not tracked is a no-op (idempotent DELETE).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final UserFlightTrackingRepository trackingRepository;
    private final MockFlightDataEngine dataEngine;
    private final FlightStatusService flightStatusService;

    // ────────────────────────────────────────────────────────────────────
    //  Track
    // ────────────────────────────────────────────────────────────────────

    /**
     * Adds a flight to the user's tracking list.
     *
     * @param userId       identifier of the requesting user
     * @param flightNumber IATA flight number to track
     * @return the live status of the newly tracked flight
     * @throws FlightNotFoundException if the flight is not in the data engine
     * @throws TrackingException       if the user is already tracking this flight
     */
    @Transactional
    public FlightStatusResponse trackFlight(String userId, String flightNumber) {
        String key = flightNumber.toUpperCase();

        // ① Validate: flight must exist in the engine
        FlightInfo flight = dataEngine.getFlightInfo(key)
                .orElseThrow(() -> new FlightNotFoundException(
                        "Flight not found in live system: " + key));

        // ② Validate: no duplicate tracking entry
        if (trackingRepository.existsByUserIdAndFlightNumber(userId, key)) {
            throw new TrackingException(
                    "User " + userId + " is already tracking flight " + key);
        }

        // ③ Persist tracking record
        UserFlightTracking record = UserFlightTracking.builder()
                .userId(userId)
                .flightNumber(key)
                .trackedAt(LocalDateTime.now())
                .build();

        trackingRepository.save(record);
        log.info("User {} started tracking flight {}", userId, key);

        // ④ Return live status so the frontend can immediately render the card
        return flightStatusService.toResponse(flight);
    }

    // ────────────────────────────────────────────────────────────────────
    //  Untrack
    // ────────────────────────────────────────────────────────────────────

    /**
     * Removes a flight from the user's tracking list (idempotent).
     *
     * @param userId       identifier of the requesting user
     * @param flightNumber IATA flight number to stop tracking
     */
    @Transactional
    public void untrackFlight(String userId, String flightNumber) {
        String key = flightNumber.toUpperCase();
        trackingRepository.deleteByUserIdAndFlightNumber(userId, key);
        log.info("User {} stopped tracking flight {}", userId, key);
    }

    // ────────────────────────────────────────────────────────────────────
    //  Query
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns live status for every flight currently tracked by a user.
     *
     * Flights that no longer exist in the data engine (edge case: seeded
     * data cleared at restart) are silently filtered out rather than
     * throwing, to avoid breaking the dashboard.
     *
     * @param userId identifier of the requesting user
     * @return ordered list of live flight statuses (may be empty)
     */
    public List<FlightStatusResponse> getTrackedFlights(String userId) {
        return trackingRepository.findByUserId(userId)
                .stream()
                .map(t -> dataEngine.getFlightInfo(t.getFlightNumber()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(flightStatusService::toResponse)
                .toList();
    }

    /**
     * Convenience check: is a given user tracking a specific flight?
     */
    public boolean isTracking(String userId, String flightNumber) {
        return trackingRepository.existsByUserIdAndFlightNumber(
                userId, flightNumber.toUpperCase());
    }
}
