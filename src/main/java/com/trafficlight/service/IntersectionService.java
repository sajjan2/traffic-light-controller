package com.trafficlight.service;

import com.trafficlight.dto.*;
import com.trafficlight.model.Direction;
import com.trafficlight.model.Intersection;
import com.trafficlight.model.LightState;
import com.trafficlight.model.StateChangeEvent;

import java.util.List;

/**
 * Service interface for managing intersections and their traffic lights.
 */
public interface IntersectionService {

    /**
     * Creates a new intersection.
     *
     * @param request the creation request
     * @return the created intersection DTO
     */
    IntersectionDto createIntersection(CreateIntersectionRequest request);

    /**
     * Gets an intersection by ID.
     *
     * @param intersectionId the intersection ID
     * @return the intersection DTO
     */
    IntersectionDto getIntersection(String intersectionId);

    /**
     * Gets all intersections.
     *
     * @return list of all intersection DTOs
     */
    List<IntersectionDto> getAllIntersections();

    /**
     * Deletes an intersection.
     *
     * @param intersectionId the intersection ID
     */
    void deleteIntersection(String intersectionId);

    /**
     * Changes the state of a traffic light at an intersection.
     *
     * @param intersectionId the intersection ID
     * @param request the state change request
     * @return the updated intersection DTO
     */
    IntersectionDto changeTrafficLightState(String intersectionId, ChangeLightStateRequest request);

    /**
     * Starts automatic operation of an intersection.
     *
     * @param intersectionId the intersection ID
     * @return the updated intersection DTO
     */
    IntersectionDto startIntersection(String intersectionId);

    /**
     * Pauses operation of an intersection.
     *
     * @param intersectionId the intersection ID
     * @return the updated intersection DTO
     */
    IntersectionDto pauseIntersection(String intersectionId);

    /**
     * Resumes operation of a paused intersection.
     *
     * @param intersectionId the intersection ID
     * @return the updated intersection DTO
     */
    IntersectionDto resumeIntersection(String intersectionId);

    /**
     * Triggers an emergency stop at an intersection.
     *
     * @param intersectionId the intersection ID
     * @return the updated intersection DTO
     */
    IntersectionDto emergencyStop(String intersectionId);

    /**
     * Updates the timing configuration for an intersection.
     *
     * @param intersectionId the intersection ID
     * @param timingConfig the new timing configuration
     * @return the updated intersection DTO
     */
    IntersectionDto updateTimingConfig(String intersectionId, TimingConfigDto timingConfig);

    /**
     * Gets the state change history for an intersection.
     *
     * @param intersectionId the intersection ID
     * @return list of state change events
     */
    List<StateChangeEventDto> getStateHistory(String intersectionId);

    /**
     * Gets the state change history for a specific direction at an intersection.
     *
     * @param intersectionId the intersection ID
     * @param direction the direction
     * @return list of state change events for that direction
     */
    List<StateChangeEventDto> getStateHistoryForDirection(String intersectionId, Direction direction);

    /**
     * Gets the most recent state change events for an intersection.
     *
     * @param intersectionId the intersection ID
     * @param count the number of events to retrieve
     * @return list of recent state change events
     */
    List<StateChangeEventDto> getRecentHistory(String intersectionId, int count);

    /**
     * Clears the state history for an intersection.
     *
     * @param intersectionId the intersection ID
     */
    void clearHistory(String intersectionId);

    /**
     * Gets the current state of a specific traffic light.
     *
     * @param intersectionId the intersection ID
     * @param direction the direction
     * @return the traffic light state DTO
     */
    TrafficLightStateDto getTrafficLightState(String intersectionId, Direction direction);

    /**
     * Gets the raw Intersection model (for internal use).
     *
     * @param intersectionId the intersection ID
     * @return the Intersection model
     */
    Intersection getIntersectionModel(String intersectionId);
}
