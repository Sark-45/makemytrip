package com.makemytrip.flight.enums;

/**
 * Classifies the kind of real-time push notification sent
 * over WebSocket to subscribed frontend clients.
 */
public enum NotificationType {

    /** A delay has been introduced or the delay duration has increased. */
    DELAY_UPDATE,

    /** Boarding has commenced at the gate. */
    BOARDING_STARTED,

    /** The scheduled departure time has shifted. */
    DEPARTURE_CHANGE,

    /** The estimated arrival time has been recalculated. */
    ETA_UPDATE,

    /** Flight status returned to ON_TIME. */
    STATUS_NORMALISED,

    /** Flight has been cancelled. */
    CANCELLATION
}
