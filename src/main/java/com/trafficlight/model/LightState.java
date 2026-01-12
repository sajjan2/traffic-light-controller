package com.trafficlight.model;

/**
 * Enumeration representing the possible states of a traffic light.
 */
public enum LightState {
    RED("Stop - Do not proceed"),
    YELLOW("Caution - Prepare to stop"),
    GREEN("Go - Proceed with caution");

    private final String description;

    LightState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the next state in the normal traffic light sequence.
     * GREEN -> YELLOW -> RED -> GREEN
     *
     * @return the next LightState in sequence
     */
    public LightState getNextState() {
        return switch (this) {
            case GREEN -> YELLOW;
            case YELLOW -> RED;
            case RED -> GREEN;
        };
    }
}
