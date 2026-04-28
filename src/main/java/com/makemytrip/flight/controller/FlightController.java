package com.makemytrip.flight.controller;

import com.makemytrip.flight.dto.*;
import com.makemytrip.flight.service.FlightStatusService;
import com.makemytrip.flight.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  FlightController
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  Exposes the Live Flight Status Tracking REST API.
 *
 *  Base path: /api/flights
 *
 *  Endpoint summary
 *  ─────────────────────────────────────────────────────────────────────
 *  GET    /api/flights/{flightNumber}            → live status
 *  GET    /api/flights/{flightNumber}/history    → full status timeline
 *  GET    /api/flights/{flightNumber}/history/recent → last 5 entries
 *  GET    /api/flights/all                       → all seeded flights
 *  POST   /api/flights/track                     → add to tracking list
 *  DELETE /api/flights/untrack                   → remove from tracking list
 *  GET    /api/flights/user/{userId}             → all tracked with live status
 *  ─────────────────────────────────────────────────────────────────────
 *
 *  All responses are wrapped in {@link ApiResponse} for a consistent envelope.
 *  Validation errors and business exceptions are handled by
 *  {@link com.makemytrip.flight.exception.GlobalExceptionHandler}.
 */
@Slf4j
@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Tighten to frontend origin in production
public class FlightController {

    private final FlightStatusService flightStatusService;
    private final TrackingService trackingService;

    // ────────────────────────────────────────────────────────────────────
    //  1. GET /api/flights/{flightNumber}  →  live status of one flight
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns the live operational status of a single flight.
     *
     * Response is cached for 30 s (evicted by the scheduler on each
     * simulated update) so repeated polling is inexpensive.
     *
     * Example: GET /api/flights/AI302
     */
    @GetMapping("/{flightNumber}")
    public ResponseEntity<ApiResponse<FlightStatusResponse>> getFlightStatus(
            @PathVariable String flightNumber) {

        log.debug("GET /api/flights/{}", flightNumber);
        FlightStatusResponse response = flightStatusService.getFlightStatus(
                flightNumber.toUpperCase());
        return ResponseEntity.ok(
                ApiResponse.ok("Flight status retrieved successfully", response));
    }

    // ────────────────────────────────────────────────────────────────────
    //  2. GET /api/flights/{flightNumber}/history  →  full status timeline
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns every status-change entry for the given flight (oldest first).
     * Useful for rendering a timeline widget in the frontend.
     *
     * Example: GET /api/flights/AI302/history
     */
    @GetMapping("/{flightNumber}/history")
    public ResponseEntity<ApiResponse<List<FlightStatusHistoryResponse>>> getHistory(
            @PathVariable String flightNumber) {

        List<FlightStatusHistoryResponse> history =
                flightStatusService.getStatusHistory(flightNumber.toUpperCase());
        return ResponseEntity.ok(
                ApiResponse.ok("Status history retrieved", history));
    }

    // ────────────────────────────────────────────────────────────────────
    //  3. GET /api/flights/{flightNumber}/history/recent  →  last 5 changes
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns the 5 most recent status entries (newest first).
     * Optimised for the "Recent Updates" panel in the dashboard.
     *
     * Example: GET /api/flights/AI302/history/recent
     */
    @GetMapping("/{flightNumber}/history/recent")
    public ResponseEntity<ApiResponse<List<FlightStatusHistoryResponse>>> getRecentHistory(
            @PathVariable String flightNumber) {

        List<FlightStatusHistoryResponse> history =
                flightStatusService.getRecentHistory(flightNumber.toUpperCase());
        return ResponseEntity.ok(
                ApiResponse.ok("Recent history retrieved", history));
    }

    // ────────────────────────────────────────────────────────────────────
    //  4. GET /api/flights/all  →  all flights in the data engine
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns the live status of every flight currently seeded in the
     * mock data engine.  Handy for the initial dashboard load.
     *
     * Example: GET /api/flights/all
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FlightStatusResponse>>> getAllFlights() {

        // Delegate to service; engine exposes a snapshot of all flights
        List<FlightStatusResponse> all = flightStatusService
                .getAllFlightStatuses(); // added convenience method below
        return ResponseEntity.ok(
                ApiResponse.ok("All flights retrieved", all));
    }

    // ────────────────────────────────────────────────────────────────────
    //  5. POST /api/flights/track  →  add a flight to user's tracking list
    // ────────────────────────────────────────────────────────────────────

    /**
     * Adds the specified flight to the user's tracking list and
     * returns the flight's current live status so the frontend can
     * render a card immediately.
     *
     * Request body:
     * <pre>
     * { "userId": "user123", "flightNumber": "AI302" }
     * </pre>
     *
     * Returns 201 Created on success.
     * Returns 409 Conflict if already tracking.
     * Returns 404 Not Found if flight does not exist.
     */
    @PostMapping("/track")
    public ResponseEntity<ApiResponse<FlightStatusResponse>> trackFlight(
            @Valid @RequestBody TrackFlightRequest request) {

        log.info("POST /api/flights/track → user={}, flight={}",
                 request.getUserId(), request.getFlightNumber());

        FlightStatusResponse status = trackingService.trackFlight(
                request.getUserId(),
                request.getFlightNumber().toUpperCase());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Flight tracking started", status));
    }

    // ────────────────────────────────────────────────────────────────────
    //  6. DELETE /api/flights/untrack  →  remove from tracking list
    // ────────────────────────────────────────────────────────────────────

    /**
     * Removes a flight from the user's tracking list.  Idempotent:
     * calling it on a flight that is not tracked returns 200.
     *
     * Request body:
     * <pre>
     * { "userId": "user123", "flightNumber": "AI302" }
     * </pre>
     */
    @DeleteMapping("/untrack")
    public ResponseEntity<ApiResponse<Void>> untrackFlight(
            @Valid @RequestBody UntrackFlightRequest request) {

        log.info("DELETE /api/flights/untrack → user={}, flight={}",
                 request.getUserId(), request.getFlightNumber());

        trackingService.untrackFlight(
                request.getUserId(),
                request.getFlightNumber().toUpperCase());

        return ResponseEntity.ok(
                ApiResponse.ok("Flight tracking removed"));
    }

    // ────────────────────────────────────────────────────────────────────
    //  7. GET /api/flights/user/{userId}  →  all tracked flights with live status
    // ────────────────────────────────────────────────────────────────────

    /**
     * Returns live status for every flight the user is currently tracking.
     *
     * This is the primary endpoint polled by the "My Flights" dashboard.
     * The response is a flat list; the frontend groups and sorts as needed.
     *
     * Example: GET /api/flights/user/user123
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<FlightStatusResponse>>> getTrackedFlights(
            @PathVariable String userId) {

        log.debug("GET /api/flights/user/{}", userId);
        List<FlightStatusResponse> tracked = trackingService.getTrackedFlights(userId);
        return ResponseEntity.ok(
                ApiResponse.ok("Tracked flights retrieved", tracked));
    }
}
