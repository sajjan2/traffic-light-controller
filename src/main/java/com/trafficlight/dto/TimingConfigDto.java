package com.trafficlight.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for timing configuration of traffic lights.
 */
public record TimingConfigDto(
        @NotNull(message = "Green duration is required")
        @Min(value = 1000, message = "Green duration must be at least 1000ms")
        Long greenDurationMs,
        
        @NotNull(message = "Yellow duration is required")
        @Min(value = 1000, message = "Yellow duration must be at least 1000ms")
        Long yellowDurationMs,
        
        @NotNull(message = "Red duration is required")
        @Min(value = 1000, message = "Red duration must be at least 1000ms")
        Long redDurationMs
) {
    /**
     * Creates a default timing configuration.
     */
    public static TimingConfigDto defaultConfig() {
        return new TimingConfigDto(30000L, 5000L, 35000L);
    }
}
