package com.makemytrip.flight.service;

import com.makemytrip.flight.dto.FlightStatusHistoryResponse;
import com.makemytrip.flight.dto.FlightStatusResponse;
import com.makemytrip.flight.engine.MockFlightDataEngine;
import com.makemytrip.flight.exception.FlightNotFoundException;
import com.makemytrip.flight.model.FlightInfo;
import com.makemytrip.flight.model.FlightStatusHistory;
import com.makemytrip.flight.repository.FlightStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  FlightStatusService
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  Owns all business logic related to reading flight status and maintaining
 *  the audit history.  It is the only class that should transform a
 *  {@link FlightInfo} domain object into a {@link FlightStatusResponse} DTO.
 *
 *  Caching strategy:
 *  - Results are cached in Caffeine for 30 s (configured in application.properties).
 *  - The scheduler calls {@link #evictCache(String)} after each update so
 *    the next REST read always sees fresh data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlightStatusService {

    private final MockFlightDataEngine dataEngine;
    private final FlightStatusHistoryRepository historyRepository;

    // ────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns the live status of a single flight.
     *
     * The result is cached for 30 seconds.  Cache is evicted by the
     * scheduler after every simulated update so staleness is bounded.
     *
     * @param flightNumber IATA flight number (case-insensitive)
     * @throws FlightNotFoundException if the flight is not tracked by the engine
     */
    @Cacheable(value = "flightStatus", key = "#flightNumber.toUpperCase()")
    public FlightStatusResponse getFlightStatus(String flightNumber) {
        log.debug("Cache miss – fetching live status for {}", flightNumber);
        FlightInfo flight = dataEngine.getFlightInfo(flightNumber)
                .orElseThrow(() -> new FlightNotFoundException(
                        "Flight not found: " + flightNumber));
        return toResponse(flight);
    }

    /**
     * Returns live status for ALL flights in the data engine.
     * Used by GET /api/flights/all for the initial dashboard load.
     */
    public List<FlightStatusResponse> getAllFlightStatuses() {
        return dataEngine.getAllFlights()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns the full status-change timeline for a flight (oldest first).
     *
     * @param flightNumber IATA flight number (case-insensitive)
     */
    public List<FlightStatusHistoryResponse> getStatusHistory(String flightNumber) {
        return historyRepository
                .findByFlightNumberOrderByRecordedAtAsc(flightNumber.toUpperCase())
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    /**
     * Returns the latest 5 history entries for a flight (most-recent first).
     */
    public List<FlightStatusHistoryResponse> getRecentHistory(String flightNumber) {
        return historyRepository
                .findTop5ByFlightNumberOrderByRecordedAtDesc(flightNumber.toUpperCase())
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    /**
     * Appends a history record and evicts the cache for this flight.
     * Called by {@link com.makemytrip.flight.scheduler.FlightStatusSimulator}
     * after each simulated state change.
     *
     * @param previous snapshot before the update (used to build the change description)
     * @param current  snapshot after the update
     */
    @CacheEvict(value = "flightStatus", key = "#current.flightNumber.toUpperCase()")
    public void recordHistoryAndEvictCache(FlightInfo previous, FlightInfo current) {
        String description = buildChangeDescription(previous, current);

        FlightStatusHistory entry = FlightStatusHistory.builder()
                .flightNumber(current.getFlightNumber())
                .status(current.getStatus())
                .delayMinutes(current.getDelayMinutes())
                .delayReason(current.getDelayReason())
                .estimatedArrival(current.getEstimatedArrival())
                .recordedAt(LocalDateTime.now())
                .changeDescription(description)
                .build();

        historyRepository.save(entry);
        log.debug("History recorded for {}: {}", current.getFlightNumber(), description);
    }

    // ────────────────────────────────────────────────────────────────────
    //  Mapping helpers
    // ────────────────────────────────────────────────────────────────────

    /**
     * Converts a {@link FlightInfo} domain object to the API response DTO.
     * Computes the live {@code minutesToDeparture} countdown.
     */
    public FlightStatusResponse toResponse(FlightInfo f) {
        long minutesToDep = Duration
                .between(LocalDateTime.now(), f.getEstimatedDeparture())
                .toMinutes();

        return FlightStatusResponse.builder()
                .flightNumber(f.getFlightNumber())
                .airline(f.getAirline())
                .origin(f.getOrigin())
                .destination(f.getDestination())
                .status(f.getStatus())
                .delayMinutes(f.getDelayMinutes())
                .delayReason(f.getDelayReason())
                .delayReasonDescription(
                    f.getDelayReason() != null
                        ? f.getDelayReason().getDescription()
                        : null)
                .scheduledDeparture(f.getScheduledDeparture())
                .estimatedDeparture(f.getEstimatedDeparture())
                .estimatedArrival(f.getEstimatedArrival())
                .gate(f.getGate())
                .terminal(f.getTerminal())
                .aircraftType(f.getAircraftType())
                .lastUpdated(f.getLastUpdated())
                .minutesToDeparture(minutesToDep)
                .build();
    }

    private FlightStatusHistoryResponse toHistoryResponse(FlightStatusHistory h) {
        return FlightStatusHistoryResponse.builder()
                .flightNumber(h.getFlightNumber())
                .status(h.getStatus())
                .delayMinutes(h.getDelayMinutes())
                .delayReason(h.getDelayReason())
                .changeDescription(h.getChangeDescription())
                .estimatedArrival(h.getEstimatedArrival())
                .recordedAt(h.getRecordedAt())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ────────────────────────────────────────────────────────────────────

    /**
     * Generates a human-readable description of what changed between
     * two consecutive snapshots.
     */
    private String buildChangeDescription(FlightInfo prev, FlightInfo curr) {
        if (prev == null) {
            return String.format("Flight %s initialised with status %s.",
                    curr.getFlightNumber(), curr.getStatus());
        }

        if (prev.getStatus() != curr.getStatus()) {
            return String.format("Status changed from %s to %s.",
                    prev.getStatus(), curr.getStatus());
        }

        if (prev.getDelayMinutes() != curr.getDelayMinutes()) {
            int delta = curr.getDelayMinutes() - prev.getDelayMinutes();
            return delta > 0
                ? String.format("Delay increased by %d min (total %d min): %s.",
                        delta, curr.getDelayMinutes(),
                        curr.getDelayReason().getDescription())
                : String.format("Delay reduced by %d min (total %d min).",
                        -delta, curr.getDelayMinutes());
        }

        return "Flight data refreshed — no status change.";
    }
}
