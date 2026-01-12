package com.trafficlight.model;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents an intersection with multiple traffic lights.
 * Thread-safe implementation supporting concurrent access.
 */
public class Intersection {

    private final String id;
    private final String name;
    private final Map<Direction, TrafficLight> trafficLights;
    private final AtomicReference<OperationStatus> operationStatus;
    private final List<StateChangeEvent> stateHistory;
    private final ReentrantReadWriteLock historyLock;
    private final Instant createdAt;
    private final AtomicReference<Instant> lastModifiedAt;
    
    // Configuration
    private volatile long greenDurationMs;
    private volatile long yellowDurationMs;
    private volatile long redDurationMs;
    
    // Maximum history size to prevent memory issues
    private static final int MAX_HISTORY_SIZE = 1000;

    /**
     * Creates a new Intersection with the specified ID and name.
     * Initializes traffic lights for all four directions.
     *
     * @param id unique identifier for the intersection
     * @param name human-readable name for the intersection
     */
    public Intersection(String id, String name) {
        this.id = Objects.requireNonNull(id, "Intersection ID cannot be null");
        this.name = Objects.requireNonNull(name, "Intersection name cannot be null");
        this.trafficLights = new ConcurrentHashMap<>();
        this.operationStatus = new AtomicReference<>(OperationStatus.PAUSED);
        this.stateHistory = new CopyOnWriteArrayList<>();
        this.historyLock = new ReentrantReadWriteLock();
        this.createdAt = Instant.now();
        this.lastModifiedAt = new AtomicReference<>(this.createdAt);
        
        // Default timing configuration
        this.greenDurationMs = 30000;  // 30 seconds
        this.yellowDurationMs = 5000;  // 5 seconds
        this.redDurationMs = 35000;    // 35 seconds
        
        // Initialize all traffic lights with RED state for safety
        for (Direction direction : Direction.values()) {
            trafficLights.put(direction, new TrafficLight(direction, LightState.RED));
        }
    }

    /**
     * Gets the intersection ID.
     *
     * @return the intersection ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the intersection name.
     *
     * @return the intersection name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the traffic light for a specific direction.
     *
     * @param direction the direction
     * @return the TrafficLight for that direction
     */
    public TrafficLight getTrafficLight(Direction direction) {
        return trafficLights.get(direction);
    }

    /**
     * Gets all traffic lights at this intersection.
     *
     * @return unmodifiable map of direction to traffic light
     */
    public Map<Direction, TrafficLight> getAllTrafficLights() {
        return Collections.unmodifiableMap(trafficLights);
    }

    /**
     * Gets the current operation status.
     *
     * @return the current OperationStatus
     */
    public OperationStatus getOperationStatus() {
        return operationStatus.get();
    }

    /**
     * Sets the operation status.
     *
     * @param status the new operation status
     */
    public void setOperationStatus(OperationStatus status) {
        operationStatus.set(Objects.requireNonNull(status, "Operation status cannot be null"));
        lastModifiedAt.set(Instant.now());
    }

    /**
     * Checks if the intersection is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return operationStatus.get() == OperationStatus.RUNNING;
    }

    /**
     * Checks if the intersection is currently paused.
     *
     * @return true if paused
     */
    public boolean isPaused() {
        return operationStatus.get() == OperationStatus.PAUSED;
    }

    /**
     * Changes the state of a traffic light and records the event.
     * Validates that the change doesn't create a conflict.
     *
     * @param direction the direction to change
     * @param newState the new state
     * @param triggeredBy who/what triggered the change
     * @return true if the change was successful
     * @throws IllegalStateException if the change would create a conflict
     */
    public boolean changeTrafficLightState(Direction direction, LightState newState, String triggeredBy) {
        TrafficLight light = trafficLights.get(direction);
        if (light == null) {
            throw new IllegalArgumentException("No traffic light for direction: " + direction);
        }

        // Validate no conflict if setting to GREEN
        if (newState == LightState.GREEN) {
            validateNoConflict(direction);
        }

        LightState previousState = light.getCurrentState();
        long duration = light.getDurationInCurrentState();
        
        light.changeState(newState);
        
        // Record the state change event
        StateChangeEvent event = StateChangeEvent.create(
                id, direction, previousState, newState, duration, triggeredBy
        );
        addToHistory(event);
        lastModifiedAt.set(Instant.now());
        
        return true;
    }

    /**
     * Validates that setting a direction to GREEN won't conflict with other green lights.
     *
     * @param direction the direction to validate
     * @throws IllegalStateException if a conflict would occur
     */
    public void validateNoConflict(Direction direction) {
        for (Map.Entry<Direction, TrafficLight> entry : trafficLights.entrySet()) {
            Direction otherDirection = entry.getKey();
            TrafficLight otherLight = entry.getValue();
            
            if (direction.conflictsWith(otherDirection) && otherLight.isGreen()) {
                throw new IllegalStateException(
                        String.format("Cannot set %s to GREEN: conflicting direction %s is already GREEN",
                                direction, otherDirection)
                );
            }
        }
    }

    /**
     * Checks if there are any conflicting green lights.
     *
     * @return true if there's a conflict
     */
    public boolean hasConflict() {
        List<Direction> greenDirections = trafficLights.entrySet().stream()
                .filter(e -> e.getValue().isGreen())
                .map(Map.Entry::getKey)
                .toList();

        for (int i = 0; i < greenDirections.size(); i++) {
            for (int j = i + 1; j < greenDirections.size(); j++) {
                if (greenDirections.get(i).conflictsWith(greenDirections.get(j))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the state change history.
     *
     * @return unmodifiable list of state change events
     */
    public List<StateChangeEvent> getStateHistory() {
        return Collections.unmodifiableList(stateHistory);
    }

    /**
     * Gets the state change history for a specific direction.
     *
     * @param direction the direction to filter by
     * @return list of state change events for that direction
     */
    public List<StateChangeEvent> getStateHistoryForDirection(Direction direction) {
        return stateHistory.stream()
                .filter(event -> event.direction() == direction)
                .toList();
    }

    /**
     * Gets the most recent state change events.
     *
     * @param count the number of events to retrieve
     * @return list of recent state change events
     */
    public List<StateChangeEvent> getRecentHistory(int count) {
        int size = stateHistory.size();
        int fromIndex = Math.max(0, size - count);
        return stateHistory.subList(fromIndex, size);
    }

    /**
     * Adds an event to the history, maintaining the maximum size.
     *
     * @param event the event to add
     */
    private void addToHistory(StateChangeEvent event) {
        historyLock.writeLock().lock();
        try {
            stateHistory.add(event);
            // Trim history if it exceeds maximum size
            while (stateHistory.size() > MAX_HISTORY_SIZE) {
                stateHistory.remove(0);
            }
        } finally {
            historyLock.writeLock().unlock();
        }
    }

    /**
     * Clears the state history.
     */
    public void clearHistory() {
        historyLock.writeLock().lock();
        try {
            stateHistory.clear();
        } finally {
            historyLock.writeLock().unlock();
        }
    }

    /**
     * Gets the creation timestamp.
     *
     * @return the creation Instant
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the last modification timestamp.
     *
     * @return the last modification Instant
     */
    public Instant getLastModifiedAt() {
        return lastModifiedAt.get();
    }

    // Timing configuration getters and setters
    
    public long getGreenDurationMs() {
        return greenDurationMs;
    }

    public void setGreenDurationMs(long greenDurationMs) {
        if (greenDurationMs <= 0) {
            throw new IllegalArgumentException("Green duration must be positive");
        }
        this.greenDurationMs = greenDurationMs;
        lastModifiedAt.set(Instant.now());
    }

    public long getYellowDurationMs() {
        return yellowDurationMs;
    }

    public void setYellowDurationMs(long yellowDurationMs) {
        if (yellowDurationMs <= 0) {
            throw new IllegalArgumentException("Yellow duration must be positive");
        }
        this.yellowDurationMs = yellowDurationMs;
        lastModifiedAt.set(Instant.now());
    }

    public long getRedDurationMs() {
        return redDurationMs;
    }

    public void setRedDurationMs(long redDurationMs) {
        if (redDurationMs <= 0) {
            throw new IllegalArgumentException("Red duration must be positive");
        }
        this.redDurationMs = redDurationMs;
        lastModifiedAt.set(Instant.now());
    }

    /**
     * Sets all lights to RED (emergency stop).
     *
     * @param triggeredBy who/what triggered the emergency stop
     */
    public void emergencyStop(String triggeredBy) {
        for (Direction direction : Direction.values()) {
            TrafficLight light = trafficLights.get(direction);
            if (!light.isRed()) {
                changeTrafficLightState(direction, LightState.RED, triggeredBy + " (EMERGENCY)");
            }
        }
        setOperationStatus(OperationStatus.EMERGENCY);
    }

    /**
     * Gets a snapshot of the current state of all traffic lights.
     *
     * @return map of direction to current light state
     */
    public Map<Direction, LightState> getCurrentStateSnapshot() {
        Map<Direction, LightState> snapshot = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, TrafficLight> entry : trafficLights.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().getCurrentState());
        }
        return snapshot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Intersection that = (Intersection) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Intersection{id='%s', name='%s', status=%s}",
                id, name, operationStatus.get());
    }
}
