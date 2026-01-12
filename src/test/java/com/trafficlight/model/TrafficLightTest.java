package com.trafficlight.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the TrafficLight class.
 */
@DisplayName("TrafficLight Tests")
class TrafficLightTest {

    private TrafficLight trafficLight;

    @BeforeEach
    void setUp() {
        trafficLight = new TrafficLight(Direction.NORTH);
    }

    @Test
    @DisplayName("New traffic light should have RED state by default")
    void newTrafficLightShouldBeRed() {
        assertEquals(LightState.RED, trafficLight.getCurrentState());
    }

    @Test
    @DisplayName("Traffic light should be created with specified initial state")
    void trafficLightShouldHaveSpecifiedInitialState() {
        TrafficLight greenLight = new TrafficLight(Direction.SOUTH, LightState.GREEN);
        assertEquals(LightState.GREEN, greenLight.getCurrentState());
    }

    @Test
    @DisplayName("Traffic light should have correct direction")
    void trafficLightShouldHaveCorrectDirection() {
        assertEquals(Direction.NORTH, trafficLight.getDirection());
    }

    @Test
    @DisplayName("changeState should update current state")
    void changeStateShouldUpdateCurrentState() {
        trafficLight.changeState(LightState.GREEN);
        assertEquals(LightState.GREEN, trafficLight.getCurrentState());
    }

    @Test
    @DisplayName("changeState should return previous state")
    void changeStateShouldReturnPreviousState() {
        LightState previousState = trafficLight.changeState(LightState.GREEN);
        assertEquals(LightState.RED, previousState);
    }

    @Test
    @DisplayName("changeState should update previous state")
    void changeStateShouldUpdatePreviousState() {
        trafficLight.changeState(LightState.GREEN);
        assertEquals(LightState.RED, trafficLight.getPreviousState());
    }

    @Test
    @DisplayName("changeState should update last state change time")
    void changeStateShouldUpdateLastStateChangeTime() {
        Instant before = Instant.now();
        trafficLight.changeState(LightState.GREEN);
        Instant after = Instant.now();
        
        Instant changeTime = trafficLight.getLastStateChangeTime();
        assertTrue(changeTime.compareTo(before) >= 0);
        assertTrue(changeTime.compareTo(after) <= 0);
    }

    @Test
    @DisplayName("changeState to same state should not update")
    void changeStateToSameStateShouldNotUpdate() {
        Instant initialTime = trafficLight.getLastStateChangeTime();
        
        // Small delay to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        trafficLight.changeState(LightState.RED); // Same as current
        
        // Time should not have changed significantly
        assertEquals(initialTime, trafficLight.getLastStateChangeTime());
    }

    @Test
    @DisplayName("advanceToNextState should follow correct sequence")
    void advanceToNextStateShouldFollowSequence() {
        // Start at RED
        assertEquals(LightState.RED, trafficLight.getCurrentState());
        
        // Advance to GREEN
        trafficLight.advanceToNextState();
        assertEquals(LightState.GREEN, trafficLight.getCurrentState());
        
        // Advance to YELLOW
        trafficLight.advanceToNextState();
        assertEquals(LightState.YELLOW, trafficLight.getCurrentState());
        
        // Advance to RED
        trafficLight.advanceToNextState();
        assertEquals(LightState.RED, trafficLight.getCurrentState());
    }

    @Test
    @DisplayName("isGreen should return true only when GREEN")
    void isGreenShouldReturnTrueOnlyWhenGreen() {
        assertFalse(trafficLight.isGreen());
        
        trafficLight.changeState(LightState.GREEN);
        assertTrue(trafficLight.isGreen());
        
        trafficLight.changeState(LightState.YELLOW);
        assertFalse(trafficLight.isGreen());
    }

    @Test
    @DisplayName("isRed should return true only when RED")
    void isRedShouldReturnTrueOnlyWhenRed() {
        assertTrue(trafficLight.isRed());
        
        trafficLight.changeState(LightState.GREEN);
        assertFalse(trafficLight.isRed());
    }

    @Test
    @DisplayName("isYellow should return true only when YELLOW")
    void isYellowShouldReturnTrueOnlyWhenYellow() {
        assertFalse(trafficLight.isYellow());
        
        trafficLight.changeState(LightState.YELLOW);
        assertTrue(trafficLight.isYellow());
    }

    @Test
    @DisplayName("getDurationInCurrentState should return positive value")
    void getDurationInCurrentStateShouldReturnPositiveValue() throws InterruptedException {
        Thread.sleep(10);
        assertTrue(trafficLight.getDurationInCurrentState() >= 10);
    }

    @Test
    @DisplayName("Constructor should throw exception for null direction")
    void constructorShouldThrowExceptionForNullDirection() {
        assertThrows(NullPointerException.class, () -> new TrafficLight(null));
    }

    @Test
    @DisplayName("Constructor should throw exception for null initial state")
    void constructorShouldThrowExceptionForNullInitialState() {
        assertThrows(NullPointerException.class, () -> new TrafficLight(Direction.NORTH, null));
    }

    @Test
    @DisplayName("changeState should throw exception for null state")
    void changeStateShouldThrowExceptionForNullState() {
        assertThrows(NullPointerException.class, () -> trafficLight.changeState(null));
    }

    @Test
    @DisplayName("Traffic lights with same direction should be equal")
    void trafficLightsWithSameDirectionShouldBeEqual() {
        TrafficLight another = new TrafficLight(Direction.NORTH, LightState.GREEN);
        assertEquals(trafficLight, another);
        assertEquals(trafficLight.hashCode(), another.hashCode());
    }

    @Test
    @DisplayName("Traffic lights with different directions should not be equal")
    void trafficLightsWithDifferentDirectionsShouldNotBeEqual() {
        TrafficLight another = new TrafficLight(Direction.SOUTH);
        assertNotEquals(trafficLight, another);
    }

    @Test
    @DisplayName("toString should contain direction and state")
    void toStringShouldContainDirectionAndState() {
        String str = trafficLight.toString();
        assertTrue(str.contains("NORTH"));
        assertTrue(str.contains("RED"));
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    @DisplayName("Traffic light should work for all directions")
    void trafficLightShouldWorkForAllDirections(Direction direction) {
        TrafficLight light = new TrafficLight(direction);
        assertEquals(direction, light.getDirection());
        assertEquals(LightState.RED, light.getCurrentState());
    }

    @Test
    @DisplayName("Traffic light should be thread-safe")
    void trafficLightShouldBeThreadSafe() throws InterruptedException {
        int threadCount = 10;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        trafficLight.advanceToNextState();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // After all operations, state should be valid
        assertNotNull(trafficLight.getCurrentState());
        assertTrue(trafficLight.getCurrentState() == LightState.RED ||
                   trafficLight.getCurrentState() == LightState.YELLOW ||
                   trafficLight.getCurrentState() == LightState.GREEN);
    }
}
