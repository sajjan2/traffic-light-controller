package com.trafficlight.dto;

import com.trafficlight.model.Direction;
import com.trafficlight.model.LightState;
import com.trafficlight.model.StateChangeEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a state change event.
 */
public record StateChangeEventDto(
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
     * Creates a StateChangeEventDto from a StateChangeEvent model.
     */
    public static StateChangeEventDto from(StateChangeEvent event) {
        return new StateChangeEventDto(
                event.eventId(),
                event.intersectionId(),
                event.direction(),
                event.previousState(),
                event.newState(),
                event.timestamp(),
                event.durationInPreviousStateMs(),
                event.triggeredBy()
        );
    }
}
