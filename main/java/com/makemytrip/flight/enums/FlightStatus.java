package com.makemytrip.flight.enums;

/**
 * Represents the possible live states of a flight.
 * Drives color-coding on the frontend and notification logic.
 */
public enum FlightStatus {

    /** Flight is operating as scheduled. */
    ON_TIME,

    /** Flight is delayed; delayMinutes and delayReason are populated. */
    DELAYED,

    /** Boarding in progress at the gate. */
    BOARDING,

    /** Flight has departed. */
    DEPARTED,

    /** Flight has landed. */
    LANDED,

    /** Flight has been cancelled. */
    CANCELLED
}
