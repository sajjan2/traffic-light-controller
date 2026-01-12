package com.trafficlight.service.impl;

import com.trafficlight.dto.*;
import com.trafficlight.exception.IntersectionAlreadyExistsException;
import com.trafficlight.exception.IntersectionNotFoundException;
import com.trafficlight.exception.InvalidOperationException;
import com.trafficlight.model.*;
import com.trafficlight.service.IntersectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the IntersectionService.
 * Thread-safe implementation using ConcurrentHashMap for intersection storage.
 */
@Service
public class IntersectionServiceImpl implements IntersectionService {

    private static final Logger logger = LoggerFactory.getLogger(IntersectionServiceImpl.class);
    
    private final Map<String, Intersection> intersections = new ConcurrentHashMap<>();

    @Override
    public IntersectionDto createIntersection(CreateIntersectionRequest request) {
        logger.info("Creating intersection with ID: {}", request.id());
        
        if (intersections.containsKey(request.id())) {
            throw new IntersectionAlreadyExistsException(request.id());
        }

        Intersection intersection = new Intersection(request.id(), request.name());
        
        // Apply timing configuration
        if (request.timingConfig() != null) {
            intersection.setGreenDurationMs(request.timingConfig().greenDurationMs());
            intersection.setYellowDurationMs(request.timingConfig().yellowDurationMs());
            intersection.setRedDurationMs(request.timingConfig().redDurationMs());
        }

        intersections.put(request.id(), intersection);
        logger.info("Intersection created successfully: {}", request.id());
        
        return IntersectionDto.from(intersection);
    }

    @Override
    public IntersectionDto getIntersection(String intersectionId) {
        return IntersectionDto.from(getIntersectionModel(intersectionId));
    }

    @Override
    public List<IntersectionDto> getAllIntersections() {
        return intersections.values().stream()
                .map(IntersectionDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteIntersection(String intersectionId) {
        logger.info("Deleting intersection: {}", intersectionId);
        
        Intersection intersection = getIntersectionModel(intersectionId);
        
        // Stop the intersection if it's running
        if (intersection.isRunning()) {
            intersection.setOperationStatus(OperationStatus.PAUSED);
        }
        
        intersections.remove(intersectionId);
        logger.info("Intersection deleted: {}", intersectionId);
    }

    @Override
    public IntersectionDto changeTrafficLightState(String intersectionId, ChangeLightStateRequest request) {
        logger.info("Changing traffic light state at intersection {}: {} -> {}",
                intersectionId, request.direction(), request.newState());
        
        Intersection intersection = getIntersectionModel(intersectionId);
        
        // Validate the state change
        if (request.newState() == LightState.GREEN) {
            intersection.validateNoConflict(request.direction());
        }
        
        intersection.changeTrafficLightState(request.direction(), request.newState(), "API_REQUEST");
        
        logger.info("Traffic light state changed successfully");
        return IntersectionDto.from(intersection);
    }

    @Override
    public IntersectionDto startIntersection(String intersectionId) {
        logger.info("Starting intersection: {}", intersectionId);
        
        Intersection intersection = getIntersectionModel(intersectionId);
        
        if (intersection.isRunning()) {
            throw new InvalidOperationException("Intersection is already running");
        }
        
        // Initialize with a safe state: NORTH/SOUTH green, EAST/WEST red
        initializeSafeState(intersection);
        intersection.setOperationStatus(OperationStatus.RUNNING);
        
        logger.info("Intersection started: {}", intersectionId);
        return IntersectionDto.from(intersection);
    }

    @Override
    public IntersectionDto pauseIntersection(String intersectionId) {
        logger.info("Pausing intersection: {}", intersectionId);
        
        Intersection intersection = getIntersectionModel(intersectionId);
        
        if (!intersection.isRunning()) {
            throw new InvalidOperationException("Intersection is not running");
        }
        
        intersection.setOperationStatus(OperationStatus.PAUSED);
        
        logger.info("Intersection paused: {}", intersectionId);
        return IntersectionDto.from(intersection);
    }

    @Override
    public IntersectionDto resumeIntersection(String intersectionId) {
        logger.info("Resuming intersection: {}", intersectionId);
        
        Intersection intersection = getIntersectionModel(intersectionId);
        
        if (intersection.getOperationStatus() != OperationStatus.PAUSED) {
            throw new InvalidOperationException("Intersection is not paused");
        }
        
        intersection.setOperationStatus(OperationStatus.RUNNING);
        
        logger.info("Intersection resumed: {}", intersectionId);
        return IntersectionDto.from(intersection);
    }

    @Override
    public IntersectionDto emergencyStop(String intersectionId) {
        logger.info("Emergency stop at intersection: {}", intersectionId);
        
        Intersection intersection = getIntersectionModel(intersectionId);
        intersection.emergencyStop("EMERGENCY_API_CALL");
        
        logger.warn("Emergency stop executed at intersection: {}", intersectionId);
        return IntersectionDto.from(intersection);
    }

    @Override
    public IntersectionDto updateTimingConfig(String intersectionId, TimingConfigDto timingConfig) {
        logger.info("Updating timing config for intersection: {}", intersectionId);
        
        Intersection intersection = getIntersectionModel(intersectionId);
        
        intersection.setGreenDurationMs(timingConfig.greenDurationMs());
        intersection.setYellowDurationMs(timingConfig.yellowDurationMs());
        intersection.setRedDurationMs(timingConfig.redDurationMs());
        
        logger.info("Timing config updated for intersection: {}", intersectionId);
        return IntersectionDto.from(intersection);
    }

    @Override
    public List<StateChangeEventDto> getStateHistory(String intersectionId) {
        Intersection intersection = getIntersectionModel(intersectionId);
        return intersection.getStateHistory().stream()
                .map(StateChangeEventDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<StateChangeEventDto> getStateHistoryForDirection(String intersectionId, Direction direction) {
        Intersection intersection = getIntersectionModel(intersectionId);
        return intersection.getStateHistoryForDirection(direction).stream()
                .map(StateChangeEventDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<StateChangeEventDto> getRecentHistory(String intersectionId, int count) {
        Intersection intersection = getIntersectionModel(intersectionId);
        return intersection.getRecentHistory(count).stream()
                .map(StateChangeEventDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public void clearHistory(String intersectionId) {
        logger.info("Clearing history for intersection: {}", intersectionId);
        Intersection intersection = getIntersectionModel(intersectionId);
        intersection.clearHistory();
        logger.info("History cleared for intersection: {}", intersectionId);
    }

    @Override
    public TrafficLightStateDto getTrafficLightState(String intersectionId, Direction direction) {
        Intersection intersection = getIntersectionModel(intersectionId);
        TrafficLight trafficLight = intersection.getTrafficLight(direction);
        return TrafficLightStateDto.from(trafficLight);
    }

    @Override
    public Intersection getIntersectionModel(String intersectionId) {
        Intersection intersection = intersections.get(intersectionId);
        if (intersection == null) {
            throw new IntersectionNotFoundException(intersectionId);
        }
        return intersection;
    }

    /**
     * Initializes the intersection with a safe state.
     * NORTH and SOUTH are set to GREEN, EAST and WEST are set to RED.
     */
    private void initializeSafeState(Intersection intersection) {
        // First, set all to RED for safety
        for (Direction direction : Direction.values()) {
            intersection.changeTrafficLightState(direction, LightState.RED, "INITIALIZATION");
        }
        
        // Then set NORTH and SOUTH to GREEN (they don't conflict)
        intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "INITIALIZATION");
        intersection.changeTrafficLightState(Direction.SOUTH, LightState.GREEN, "INITIALIZATION");
    }
}
