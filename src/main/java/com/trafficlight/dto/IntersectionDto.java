package com.trafficlight.dto;

import com.trafficlight.model.Direction;
import com.trafficlight.model.Intersection;
import com.trafficlight.model.OperationStatus;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DTO representing the complete state of an intersection.
 */
public record IntersectionDto(
        String id,
        String name,
        OperationStatus operationStatus,
        String operationStatusDescription,
        Map<Direction, TrafficLightStateDto> trafficLights,
        boolean hasConflict,
        TimingConfigDto timingConfig,
        Instant createdAt,
        Instant lastModifiedAt
) {
    /**
     * Creates an IntersectionDto from an Intersection model.
     */
    public static IntersectionDto from(Intersection intersection) {
        Map<Direction, TrafficLightStateDto> lights = intersection.getAllTrafficLights()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> TrafficLightStateDto.from(e.getValue())
                ));

        return new IntersectionDto(
                intersection.getId(),
                intersection.getName(),
                intersection.getOperationStatus(),
                intersection.getOperationStatus().getDescription(),
                lights,
                intersection.hasConflict(),
                new TimingConfigDto(
                        intersection.getGreenDurationMs(),
                        intersection.getYellowDurationMs(),
                        intersection.getRedDurationMs()
                ),
                intersection.getCreatedAt(),
                intersection.getLastModifiedAt()
        );
    }
}
