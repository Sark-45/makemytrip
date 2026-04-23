package com.makemytrip.flight.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumerated reasons for a flight delay.
 * Each reason carries a human-readable display label used
 * in notifications and the frontend dashboard.
 */
@Getter
@RequiredArgsConstructor
public enum DelayReason {

    WEATHER("Adverse weather conditions"),
    TECHNICAL_ISSUE("Technical / mechanical issue"),
    AIR_TRAFFIC("Air traffic congestion"),
    LATE_ARRIVING_AIRCRAFT("Late arriving inbound aircraft"),
    CREW_AVAILABILITY("Crew availability constraints"),
    SECURITY_CHECK("Extended security screening"),
    GROUND_HANDLING("Ground handling delay"),
    NONE("No delay");

    /** Human-readable description sent in API responses. */
    private final String description;
}
