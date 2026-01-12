package com.trafficlight.exception;

import com.trafficlight.model.Direction;

/**
 * Exception thrown when a traffic light state change would cause a conflict.
 */
public class TrafficLightConflictException extends RuntimeException {
    
    private final Direction requestedDirection;
    private final Direction conflictingDirection;

    public TrafficLightConflictException(Direction requestedDirection, Direction conflictingDirection) {
        super(String.format("Cannot set %s to GREEN: conflicting direction %s is already GREEN",
                requestedDirection, conflictingDirection));
        this.requestedDirection = requestedDirection;
        this.conflictingDirection = conflictingDirection;
    }

    public TrafficLightConflictException(String message) {
        super(message);
        this.requestedDirection = null;
        this.conflictingDirection = null;
    }

    public Direction getRequestedDirection() {
        return requestedDirection;
    }

    public Direction getConflictingDirection() {
        return conflictingDirection;
    }
}
