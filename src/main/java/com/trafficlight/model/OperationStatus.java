package com.trafficlight.model;

/**
 * Enumeration representing the operational status of an intersection.
 */
public enum OperationStatus {
    RUNNING("Intersection is operating normally"),
    PAUSED("Intersection operation is paused"),
    EMERGENCY("Emergency mode - all lights flashing"),
    MAINTENANCE("Under maintenance - manual control only");

    private final String description;

    OperationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
