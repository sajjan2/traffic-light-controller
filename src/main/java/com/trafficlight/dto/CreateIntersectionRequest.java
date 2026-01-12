package com.trafficlight.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new intersection.
 */
public record CreateIntersectionRequest(
        @NotBlank(message = "Intersection ID is required")
        @Size(min = 1, max = 50, message = "Intersection ID must be between 1 and 50 characters")
        String id,
        
        @NotBlank(message = "Intersection name is required")
        @Size(min = 1, max = 100, message = "Intersection name must be between 1 and 100 characters")
        String name,
        
        @Valid
        TimingConfigDto timingConfig
) {
    /**
     * Creates a request with default timing configuration if not provided.
     */
    public CreateIntersectionRequest {
        if (timingConfig == null) {
            timingConfig = TimingConfigDto.defaultConfig();
        }
    }
}
