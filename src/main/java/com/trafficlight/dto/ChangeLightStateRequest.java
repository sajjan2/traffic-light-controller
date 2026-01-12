package com.trafficlight.dto;

import com.trafficlight.model.Direction;
import com.trafficlight.model.LightState;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing a traffic light state.
 */
public record ChangeLightStateRequest(
        @NotNull(message = "Direction is required")
        Direction direction,
        
        @NotNull(message = "New state is required")
        LightState newState
) {
}
