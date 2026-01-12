package com.trafficlight.exception;

/**
 * Exception thrown when attempting to create an intersection that already exists.
 */
public class IntersectionAlreadyExistsException extends RuntimeException {
    
    private final String intersectionId;

    public IntersectionAlreadyExistsException(String intersectionId) {
        super(String.format("Intersection already exists with ID: %s", intersectionId));
        this.intersectionId = intersectionId;
    }

    public String getIntersectionId() {
        return intersectionId;
    }
}
