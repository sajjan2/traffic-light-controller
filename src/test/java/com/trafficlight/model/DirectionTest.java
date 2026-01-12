package com.trafficlight.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Direction enum.
 */
@DisplayName("Direction Tests")
class DirectionTest {

    @Test
    @DisplayName("NORTH should conflict with EAST and WEST")
    void northShouldConflictWithEastAndWest() {
        assertTrue(Direction.NORTH.conflictsWith(Direction.EAST));
        assertTrue(Direction.NORTH.conflictsWith(Direction.WEST));
        assertFalse(Direction.NORTH.conflictsWith(Direction.SOUTH));
        assertFalse(Direction.NORTH.conflictsWith(Direction.NORTH));
    }

    @Test
    @DisplayName("SOUTH should conflict with EAST and WEST")
    void southShouldConflictWithEastAndWest() {
        assertTrue(Direction.SOUTH.conflictsWith(Direction.EAST));
        assertTrue(Direction.SOUTH.conflictsWith(Direction.WEST));
        assertFalse(Direction.SOUTH.conflictsWith(Direction.NORTH));
        assertFalse(Direction.SOUTH.conflictsWith(Direction.SOUTH));
    }

    @Test
    @DisplayName("EAST should conflict with NORTH and SOUTH")
    void eastShouldConflictWithNorthAndSouth() {
        assertTrue(Direction.EAST.conflictsWith(Direction.NORTH));
        assertTrue(Direction.EAST.conflictsWith(Direction.SOUTH));
        assertFalse(Direction.EAST.conflictsWith(Direction.WEST));
        assertFalse(Direction.EAST.conflictsWith(Direction.EAST));
    }

    @Test
    @DisplayName("WEST should conflict with NORTH and SOUTH")
    void westShouldConflictWithNorthAndSouth() {
        assertTrue(Direction.WEST.conflictsWith(Direction.NORTH));
        assertTrue(Direction.WEST.conflictsWith(Direction.SOUTH));
        assertFalse(Direction.WEST.conflictsWith(Direction.EAST));
        assertFalse(Direction.WEST.conflictsWith(Direction.WEST));
    }

    @ParameterizedTest
    @CsvSource({
            "NORTH, SOUTH",
            "SOUTH, NORTH",
            "EAST, WEST",
            "WEST, EAST"
    })
    @DisplayName("Opposite directions should be correct")
    void oppositeShouldBeCorrect(Direction direction, Direction expectedOpposite) {
        assertEquals(expectedOpposite, direction.getOpposite());
    }

    @Test
    @DisplayName("NORTH and SOUTH should be parallel")
    void northAndSouthShouldBeParallel() {
        assertTrue(Direction.NORTH.isParallelTo(Direction.SOUTH));
        assertTrue(Direction.SOUTH.isParallelTo(Direction.NORTH));
    }

    @Test
    @DisplayName("EAST and WEST should be parallel")
    void eastAndWestShouldBeParallel() {
        assertTrue(Direction.EAST.isParallelTo(Direction.WEST));
        assertTrue(Direction.WEST.isParallelTo(Direction.EAST));
    }

    @Test
    @DisplayName("Direction should not be parallel to itself")
    void directionShouldNotBeParallelToItself() {
        for (Direction direction : Direction.values()) {
            assertFalse(direction.isParallelTo(direction));
        }
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    @DisplayName("All directions should have conflicting directions")
    void allDirectionsShouldHaveConflictingDirections(Direction direction) {
        assertNotNull(direction.getConflictingDirections());
        assertEquals(2, direction.getConflictingDirections().size());
    }

    @Test
    @DisplayName("Conflict relationship should be symmetric")
    void conflictShouldBeSymmetric() {
        for (Direction d1 : Direction.values()) {
            for (Direction d2 : Direction.values()) {
                assertEquals(d1.conflictsWith(d2), d2.conflictsWith(d1),
                        String.format("Conflict between %s and %s should be symmetric", d1, d2));
            }
        }
    }
}
