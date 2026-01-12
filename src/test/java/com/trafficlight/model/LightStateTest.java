package com.trafficlight.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the LightState enum.
 */
@DisplayName("LightState Tests")
class LightStateTest {

    @Test
    @DisplayName("GREEN should transition to YELLOW")
    void greenShouldTransitionToYellow() {
        assertEquals(LightState.YELLOW, LightState.GREEN.getNextState());
    }

    @Test
    @DisplayName("YELLOW should transition to RED")
    void yellowShouldTransitionToRed() {
        assertEquals(LightState.RED, LightState.YELLOW.getNextState());
    }

    @Test
    @DisplayName("RED should transition to GREEN")
    void redShouldTransitionToGreen() {
        assertEquals(LightState.GREEN, LightState.RED.getNextState());
    }

    @ParameterizedTest
    @EnumSource(LightState.class)
    @DisplayName("All states should have a description")
    void allStatesShouldHaveDescription(LightState state) {
        assertNotNull(state.getDescription());
        assertFalse(state.getDescription().isEmpty());
    }

    @Test
    @DisplayName("Complete cycle should return to original state")
    void completeCycleShouldReturnToOriginal() {
        LightState start = LightState.GREEN;
        LightState current = start;
        
        // Go through one complete cycle
        current = current.getNextState(); // YELLOW
        current = current.getNextState(); // RED
        current = current.getNextState(); // GREEN
        
        assertEquals(start, current);
    }
}
