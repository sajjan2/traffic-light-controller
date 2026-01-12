package com.trafficlight.model;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a single traffic light for a specific direction at an intersection.
 * Thread-safe implementation using atomic references.
 */
public class TrafficLight {
    
    private final Direction direction;
    private final AtomicReference<LightState> currentState;
    private final AtomicReference<Instant> lastStateChangeTime;
    private final AtomicReference<LightState> previousState;

    /**
     * Creates a new TrafficLight with the specified direction.
     * Initial state is RED for safety.
     *
     * @param direction the direction this traffic light controls
     */
    public TrafficLight(Direction direction) {
        this(direction, LightState.RED);
    }

    /**
     * Creates a new TrafficLight with the specified direction and initial state.
     *
     * @param direction the direction this traffic light controls
     * @param initialState the initial state of the light
     */
    public TrafficLight(Direction direction, LightState initialState) {
        this.direction = Objects.requireNonNull(direction, "Direction cannot be null");
        this.currentState = new AtomicReference<>(Objects.requireNonNull(initialState, "Initial state cannot be null"));
        this.lastStateChangeTime = new AtomicReference<>(Instant.now());
        this.previousState = new AtomicReference<>(null);
    }

    /**
     * Gets the direction this traffic light controls.
     *
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Gets the current state of the traffic light.
     *
     * @return the current LightState
     */
    public LightState getCurrentState() {
        return currentState.get();
    }

    /**
     * Gets the previous state of the traffic light.
     *
     * @return the previous LightState, or null if no state change has occurred
     */
    public LightState getPreviousState() {
        return previousState.get();
    }

    /**
     * Gets the time of the last state change.
     *
     * @return the Instant of the last state change
     */
    public Instant getLastStateChangeTime() {
        return lastStateChangeTime.get();
    }

    /**
     * Gets the duration in milliseconds since the last state change.
     *
     * @return duration in milliseconds
     */
    public long getDurationInCurrentState() {
        return Instant.now().toEpochMilli() - lastStateChangeTime.get().toEpochMilli();
    }

    /**
     * Changes the state of the traffic light.
     * Thread-safe operation using compare-and-set.
     *
     * @param newState the new state to set
     * @return the previous state before the change
     * @throws IllegalArgumentException if newState is null
     */
    public LightState changeState(LightState newState) {
        Objects.requireNonNull(newState, "New state cannot be null");
        
        LightState oldState;
        do {
            oldState = currentState.get();
            if (oldState == newState) {
                return oldState; // No change needed
            }
        } while (!currentState.compareAndSet(oldState, newState));
        
        previousState.set(oldState);
        lastStateChangeTime.set(Instant.now());
        
        return oldState;
    }

    /**
     * Advances to the next state in the normal sequence.
     * GREEN -> YELLOW -> RED -> GREEN
     *
     * @return the previous state before advancing
     */
    public LightState advanceToNextState() {
        return changeState(currentState.get().getNextState());
    }

    /**
     * Checks if this traffic light is currently green.
     *
     * @return true if the light is green
     */
    public boolean isGreen() {
        return currentState.get() == LightState.GREEN;
    }

    /**
     * Checks if this traffic light is currently red.
     *
     * @return true if the light is red
     */
    public boolean isRed() {
        return currentState.get() == LightState.RED;
    }

    /**
     * Checks if this traffic light is currently yellow.
     *
     * @return true if the light is yellow
     */
    public boolean isYellow() {
        return currentState.get() == LightState.YELLOW;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrafficLight that = (TrafficLight) o;
        return direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction);
    }

    @Override
    public String toString() {
        return String.format("TrafficLight{direction=%s, state=%s, lastChange=%s}",
                direction, currentState.get(), lastStateChangeTime.get());
    }
}
