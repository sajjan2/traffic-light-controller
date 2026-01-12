package com.trafficlight.model;

import java.util.Set;

/**
 * Enumeration representing the directions at an intersection.
 * Each direction knows which other directions conflict with it
 * (i.e., cannot be green simultaneously).
 */
public enum Direction {
    NORTH(Set.of("EAST", "WEST")),
    SOUTH(Set.of("EAST", "WEST")),
    EAST(Set.of("NORTH", "SOUTH")),
    WEST(Set.of("NORTH", "SOUTH"));

    private final Set<String> conflictingDirections;

    Direction(Set<String> conflictingDirections) {
        this.conflictingDirections = conflictingDirections;
    }

    /**
     * Checks if this direction conflicts with another direction.
     * Conflicting directions cannot both be green at the same time.
     *
     * @param other the other direction to check
     * @return true if the directions conflict, false otherwise
     */
    public boolean conflictsWith(Direction other) {
        return conflictingDirections.contains(other.name());
    }

    /**
     * Gets the set of directions that conflict with this direction.
     *
     * @return set of conflicting direction names
     */
    public Set<String> getConflictingDirections() {
        return conflictingDirections;
    }

    /**
     * Gets the opposite direction (for paired light control).
     * NORTH <-> SOUTH, EAST <-> WEST
     *
     * @return the opposite direction
     */
    public Direction getOpposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    /**
     * Checks if this direction is parallel to another (can be green together).
     *
     * @param other the other direction
     * @return true if parallel, false otherwise
     */
    public boolean isParallelTo(Direction other) {
        return !conflictsWith(other) && this != other;
    }
}
