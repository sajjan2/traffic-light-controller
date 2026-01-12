package com.trafficlight.dto;

import com.trafficlight.model.Direction;
import com.trafficlight.model.LightState;

import java.time.Instant;

/**
 * DTO representing the current state of a traffic light.
 */
public record TrafficLightStateDto(
        Direction direction,
        LightState currentState,
        String stateDescription,
        Instant lastStateChangeTime,
        long durationInCurrentStateMs
) {
    /**
     * Creates a TrafficLightStateDto from a TrafficLight model.
     */
    public static TrafficLightStateDto from(com.trafficlight.model.TrafficLight trafficLight) {
        return new TrafficLightStateDto(
                trafficLight.getDirection(),
                trafficLight.getCurrentState(),
                trafficLight.getCurrentState().getDescription(),
                trafficLight.getLastStateChangeTime(),
                trafficLight.getDurationInCurrentState()
        );
    }
}
