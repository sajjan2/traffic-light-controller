package com.trafficlight.service;

import com.trafficlight.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for automatically cycling traffic lights at running intersections.
 * Handles the timing and sequencing of light changes.
 */
@Service
public class TrafficLightScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TrafficLightScheduler.class);

    private final IntersectionService intersectionService;
    
    // Track the current phase for each intersection
    private final Map<String, AtomicReference<TrafficPhase>> intersectionPhases = new ConcurrentHashMap<>();
    private final Map<String, Long> phaseStartTimes = new ConcurrentHashMap<>();

    public TrafficLightScheduler(IntersectionService intersectionService) {
        this.intersectionService = intersectionService;
    }

    /**
     * Enum representing the phases of traffic light operation.
     */
    public enum TrafficPhase {
        NORTH_SOUTH_GREEN,      // N/S green, E/W red
        NORTH_SOUTH_YELLOW,     // N/S yellow, E/W red
        EAST_WEST_GREEN,        // E/W green, N/S red
        EAST_WEST_YELLOW        // E/W yellow, N/S red
    }

    /**
     * Scheduled task that runs every 500ms to check and update traffic light states.
     */
    @Scheduled(fixedRate = 500)
    public void processTrafficLights() {
        intersectionService.getAllIntersections().forEach(intersectionDto -> {
            if (intersectionDto.operationStatus() == OperationStatus.RUNNING) {
                processIntersection(intersectionDto.id());
            }
        });
    }

    /**
     * Processes a single intersection, advancing the traffic light sequence if needed.
     */
    private void processIntersection(String intersectionId) {
        try {
            Intersection intersection = intersectionService.getIntersectionModel(intersectionId);
            
            // Initialize phase tracking if not present
            intersectionPhases.computeIfAbsent(intersectionId, 
                    k -> new AtomicReference<>(TrafficPhase.NORTH_SOUTH_GREEN));
            phaseStartTimes.computeIfAbsent(intersectionId, k -> System.currentTimeMillis());

            AtomicReference<TrafficPhase> phaseRef = intersectionPhases.get(intersectionId);
            TrafficPhase currentPhase = phaseRef.get();
            long phaseStartTime = phaseStartTimes.get(intersectionId);
            long elapsed = System.currentTimeMillis() - phaseStartTime;

            // Determine if we need to advance to the next phase
            long phaseDuration = getPhaseDuration(currentPhase, intersection);
            
            if (elapsed >= phaseDuration) {
                advancePhase(intersection, phaseRef);
                phaseStartTimes.put(intersectionId, System.currentTimeMillis());
            }
        } catch (Exception e) {
            logger.error("Error processing intersection {}: {}", intersectionId, e.getMessage());
        }
    }

    /**
     * Gets the duration for a specific phase.
     */
    private long getPhaseDuration(TrafficPhase phase, Intersection intersection) {
        return switch (phase) {
            case NORTH_SOUTH_GREEN, EAST_WEST_GREEN -> intersection.getGreenDurationMs();
            case NORTH_SOUTH_YELLOW, EAST_WEST_YELLOW -> intersection.getYellowDurationMs();
        };
    }

    /**
     * Advances to the next phase and updates the traffic lights accordingly.
     */
    private void advancePhase(Intersection intersection, AtomicReference<TrafficPhase> phaseRef) {
        TrafficPhase currentPhase = phaseRef.get();
        TrafficPhase nextPhase = getNextPhase(currentPhase);
        
        logger.debug("Intersection {}: advancing from {} to {}", 
                intersection.getId(), currentPhase, nextPhase);

        // Apply the new phase
        applyPhase(intersection, nextPhase);
        phaseRef.set(nextPhase);
    }

    /**
     * Gets the next phase in the sequence.
     */
    private TrafficPhase getNextPhase(TrafficPhase currentPhase) {
        return switch (currentPhase) {
            case NORTH_SOUTH_GREEN -> TrafficPhase.NORTH_SOUTH_YELLOW;
            case NORTH_SOUTH_YELLOW -> TrafficPhase.EAST_WEST_GREEN;
            case EAST_WEST_GREEN -> TrafficPhase.EAST_WEST_YELLOW;
            case EAST_WEST_YELLOW -> TrafficPhase.NORTH_SOUTH_GREEN;
        };
    }

    /**
     * Applies a phase to the intersection, setting all traffic lights appropriately.
     */
    private void applyPhase(Intersection intersection, TrafficPhase phase) {
        String triggeredBy = "SCHEDULER_" + phase.name();
        
        switch (phase) {
            case NORTH_SOUTH_GREEN -> {
                intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, triggeredBy);
                intersection.changeTrafficLightState(Direction.SOUTH, LightState.GREEN, triggeredBy);
                intersection.changeTrafficLightState(Direction.EAST, LightState.RED, triggeredBy);
                intersection.changeTrafficLightState(Direction.WEST, LightState.RED, triggeredBy);
            }
            case NORTH_SOUTH_YELLOW -> {
                intersection.changeTrafficLightState(Direction.NORTH, LightState.YELLOW, triggeredBy);
                intersection.changeTrafficLightState(Direction.SOUTH, LightState.YELLOW, triggeredBy);
                // E/W stays RED
            }
            case EAST_WEST_GREEN -> {
                intersection.changeTrafficLightState(Direction.NORTH, LightState.RED, triggeredBy);
                intersection.changeTrafficLightState(Direction.SOUTH, LightState.RED, triggeredBy);
                intersection.changeTrafficLightState(Direction.EAST, LightState.GREEN, triggeredBy);
                intersection.changeTrafficLightState(Direction.WEST, LightState.GREEN, triggeredBy);
            }
            case EAST_WEST_YELLOW -> {
                intersection.changeTrafficLightState(Direction.EAST, LightState.YELLOW, triggeredBy);
                intersection.changeTrafficLightState(Direction.WEST, LightState.YELLOW, triggeredBy);
                // N/S stays RED
            }
        }
    }

    /**
     * Resets the phase tracking for an intersection.
     * Called when an intersection is stopped or deleted.
     */
    public void resetPhase(String intersectionId) {
        intersectionPhases.remove(intersectionId);
        phaseStartTimes.remove(intersectionId);
    }

    /**
     * Gets the current phase for an intersection.
     */
    public TrafficPhase getCurrentPhase(String intersectionId) {
        AtomicReference<TrafficPhase> phaseRef = intersectionPhases.get(intersectionId);
        return phaseRef != null ? phaseRef.get() : null;
    }

    /**
     * Gets the time remaining in the current phase.
     */
    public long getTimeRemainingInPhase(String intersectionId) {
        try {
            Intersection intersection = intersectionService.getIntersectionModel(intersectionId);
            AtomicReference<TrafficPhase> phaseRef = intersectionPhases.get(intersectionId);
            Long startTime = phaseStartTimes.get(intersectionId);
            
            if (phaseRef == null || startTime == null) {
                return 0;
            }
            
            long phaseDuration = getPhaseDuration(phaseRef.get(), intersection);
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0, phaseDuration - elapsed);
        } catch (Exception e) {
            return 0;
        }
    }
}
