package com.makemytrip.flight.service;

import com.makemytrip.flight.dto.FlightNotification;
import com.makemytrip.flight.enums.FlightStatus;
import com.makemytrip.flight.enums.NotificationType;
import com.makemytrip.flight.model.FlightInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ─────────────────────────────────────────────────────────────────────────
 *  NotificationService
 * ─────────────────────────────────────────────────────────────────────────
 *
 *  Converts a raw {@link FlightInfo} state change into a structured
 *  {@link FlightNotification} and broadcasts it over STOMP WebSocket
 *  to every client subscribed to:
 *
 *      /topic/flights/{flightNumber}
 *
 *  The service is deliberately thin: it owns message construction and
 *  dispatch only.  Business logic (deciding *when* to notify) lives in
 *  {@link FlightStatusService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * Spring's WebSocket messaging gateway.
     * Injected by Spring — backed by the SimpleBroker configured in
     * {@link com.makemytrip.flight.config.WebSocketConfig}.
     */
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Destination template ────────────────────────────────────────────
    private static final String FLIGHT_TOPIC = "/topic/flights/";

    // ────────────────────────────────────────────────────────────────────
    //  Public dispatch methods
    // ────────────────────────────────────────────────────────────────────

    /**
     * Build and broadcast a notification based on the new flight state.
     * Called by {@link FlightStatusService} after every status change.
     *
     * @param previous the state *before* the update (may be null on first load)
     * @param current  the state *after* the update
     */
    public void notifyStatusChange(FlightInfo previous, FlightInfo current) {
        if (current == null) return;

        FlightStatus newStatus = current.getStatus();
        NotificationType type;
        String message;

        // ─── Determine notification type and human-readable message ──────
        if (previous == null || previous.getStatus() != newStatus) {

            switch (newStatus) {
                case DELAYED -> {
                    type = NotificationType.DELAY_UPDATE;
                    message = buildDelayMessage(current);
                }
                case BOARDING -> {
                    type = NotificationType.BOARDING_STARTED;
                    message = String.format(
                        "✈ %s is now BOARDING at gate %s, %s. Please proceed.",
                        current.getFlightNumber(),
                        current.getGate(),
                        current.getTerminal());
                }
                case ON_TIME -> {
                    // Was previously delayed – now recovered
                    type = NotificationType.STATUS_NORMALISED;
                    message = String.format(
                        "✅ Good news! %s is back ON TIME. Departure: %s",
                        current.getFlightNumber(),
                        current.getEstimatedDeparture());
                }
                case DEPARTED -> {
                    type = NotificationType.DEPARTURE_CHANGE;
                    message = String.format(
                        "🛫 %s has departed from %s.",
                        current.getFlightNumber(), current.getOrigin());
                }
                case CANCELLED -> {
                    type = NotificationType.CANCELLATION;
                    message = String.format(
                        "❌ %s has been CANCELLED. Please contact %s.",
                        current.getFlightNumber(), current.getAirline());
                }
                default -> {
                    type = NotificationType.ETA_UPDATE;
                    message = String.format(
                        "ℹ %s status updated to %s.",
                        current.getFlightNumber(), newStatus);
                }
            }

        } else if (newStatus == FlightStatus.DELAYED
                && previous.getDelayMinutes() != current.getDelayMinutes()) {
            // Same DELAYED state but delay duration increased
            type = NotificationType.DELAY_UPDATE;
            message = buildDelayMessage(current);

        } else {
            // No meaningful change — send a lightweight ETA ping
            type = NotificationType.ETA_UPDATE;
            message = String.format(
                "📍 %s ETA updated: %s",
                current.getFlightNumber(), current.getEstimatedArrival());
        }

        FlightNotification notification = FlightNotification.builder()
                .flightNumber(current.getFlightNumber())
                .notificationType(type)
                .message(message)
                .newStatus(newStatus)
                .delayMinutes(current.getDelayMinutes())
                .newEstimatedDeparture(current.getEstimatedDeparture())
                .newEstimatedArrival(current.getEstimatedArrival())
                .timestamp(LocalDateTime.now())
                .build();

        broadcast(current.getFlightNumber(), notification);
    }

    /**
     * Push a pre-built notification directly (for custom scenarios).
     */
    public void send(String flightNumber, FlightNotification notification) {
        broadcast(flightNumber, notification);
    }

    // ────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ────────────────────────────────────────────────────────────────────

    private void broadcast(String flightNumber, FlightNotification notification) {
        String destination = FLIGHT_TOPIC + flightNumber.toUpperCase();
        try {
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("Pushed {} notification for {} → {}",
                      notification.getNotificationType(), flightNumber, destination);
        } catch (Exception ex) {
            log.error("Failed to push WebSocket notification for {}: {}",
                      flightNumber, ex.getMessage());
        }
    }

    private String buildDelayMessage(FlightInfo f) {
        return String.format(
            "⏱ %s DELAYED by %d min — %s. New departure: %s, ETA: %s",
            f.getFlightNumber(),
            f.getDelayMinutes(),
            f.getDelayReason().getDescription(),
            f.getEstimatedDeparture(),
            f.getEstimatedArrival());
    }
}
