package com.trafficlight.exception;

/**
 * Exception thrown when an intersection is not found.
 */
public class IntersectionNotFoundException extends RuntimeException {
    
    private final String intersectionId;

    public IntersectionNotFoundException(String intersectionId) {
        super(String.format("Intersection not found with ID: %s", intersectionId));
        this.intersectionId = intersectionId;
    }

    public String getIntersectionId() {
        return intersectionId;
    }
}
