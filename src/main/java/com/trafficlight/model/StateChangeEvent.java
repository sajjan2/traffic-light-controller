package com.trafficlight.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record representing a state change event in the traffic light system.
 * Used for tracking timing history.
 */
public record StateChangeEvent(
        UUID eventId,
        String intersectionId,
        Direction direction,
        LightState previousState,
        LightState newState,
        Instant timestamp,
        long durationInPreviousStateMs,
        String triggeredBy
) {
    /**
     * Creates a new StateChangeEvent with auto-generated ID and current timestamp.
     */
    public static StateChangeEvent create(
            String intersectionId,
            Direction direction,
            LightState previousState,
            LightState newState,
            long durationInPreviousStateMs,
            String triggeredBy
    ) {
        return new StateChangeEvent(
                UUID.randomUUID(),
                intersectionId,
                direction,
                previousState,
                newState,
                Instant.now(),
                durationInPreviousStateMs,
                triggeredBy
        );
    }
}
