package com.trafficlight.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Intersection class.
 */
@DisplayName("Intersection Tests")
class IntersectionTest {

    private Intersection intersection;

    @BeforeEach
    void setUp() {
        intersection = new Intersection("INT-001", "Main Street & 1st Avenue");
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("New intersection should have correct ID and name")
        void newIntersectionShouldHaveCorrectIdAndName() {
            assertEquals("INT-001", intersection.getId());
            assertEquals("Main Street & 1st Avenue", intersection.getName());
        }

        @Test
        @DisplayName("New intersection should have all four traffic lights")
        void newIntersectionShouldHaveAllFourTrafficLights() {
            Map<Direction, TrafficLight> lights = intersection.getAllTrafficLights();
            assertEquals(4, lights.size());
            
            for (Direction direction : Direction.values()) {
                assertNotNull(intersection.getTrafficLight(direction));
            }
        }

        @Test
        @DisplayName("All traffic lights should start RED")
        void allTrafficLightsShouldStartRed() {
            for (Direction direction : Direction.values()) {
                assertEquals(LightState.RED, intersection.getTrafficLight(direction).getCurrentState());
            }
        }

        @Test
        @DisplayName("New intersection should be PAUSED")
        void newIntersectionShouldBePaused() {
            assertEquals(OperationStatus.PAUSED, intersection.getOperationStatus());
            assertTrue(intersection.isPaused());
            assertFalse(intersection.isRunning());
        }

        @Test
        @DisplayName("New intersection should have default timing configuration")
        void newIntersectionShouldHaveDefaultTimingConfig() {
            assertEquals(30000, intersection.getGreenDurationMs());
            assertEquals(5000, intersection.getYellowDurationMs());
            assertEquals(35000, intersection.getRedDurationMs());
        }

        @Test
        @DisplayName("Constructor should throw exception for null ID")
        void constructorShouldThrowExceptionForNullId() {
            assertThrows(NullPointerException.class, () -> new Intersection(null, "Test"));
        }

        @Test
        @DisplayName("Constructor should throw exception for null name")
        void constructorShouldThrowExceptionForNullName() {
            assertThrows(NullPointerException.class, () -> new Intersection("ID", null));
        }
    }

    @Nested
    @DisplayName("State Change Tests")
    class StateChangeTests {

        @Test
        @DisplayName("Should change traffic light state successfully")
        void shouldChangeTrafficLightStateSuccessfully() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            assertEquals(LightState.GREEN, intersection.getTrafficLight(Direction.NORTH).getCurrentState());
        }

        @Test
        @DisplayName("Should record state change in history")
        void shouldRecordStateChangeInHistory() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            
            List<StateChangeEvent> history = intersection.getStateHistory();
            assertFalse(history.isEmpty());
            
            StateChangeEvent lastEvent = history.get(history.size() - 1);
            assertEquals(Direction.NORTH, lastEvent.direction());
            assertEquals(LightState.RED, lastEvent.previousState());
            assertEquals(LightState.GREEN, lastEvent.newState());
            assertEquals("TEST", lastEvent.triggeredBy());
        }

        @Test
        @DisplayName("Should update last modified time on state change")
        void shouldUpdateLastModifiedTimeOnStateChange() throws InterruptedException {
            Thread.sleep(10);
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            
            assertTrue(intersection.getLastModifiedAt().isAfter(intersection.getCreatedAt()));
        }
    }

    @Nested
    @DisplayName("Conflict Validation Tests")
    class ConflictValidationTests {

        @Test
        @DisplayName("Should throw exception when setting conflicting direction to GREEN")
        void shouldThrowExceptionWhenSettingConflictingDirectionToGreen() {
            // Set NORTH to GREEN
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            
            // Try to set EAST to GREEN (conflicts with NORTH)
            assertThrows(IllegalStateException.class, () ->
                    intersection.changeTrafficLightState(Direction.EAST, LightState.GREEN, "TEST"));
        }

        @Test
        @DisplayName("Should allow parallel directions to be GREEN simultaneously")
        void shouldAllowParallelDirectionsToBeGreenSimultaneously() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            
            // SOUTH is parallel to NORTH, should be allowed
            assertDoesNotThrow(() ->
                    intersection.changeTrafficLightState(Direction.SOUTH, LightState.GREEN, "TEST"));
            
            assertEquals(LightState.GREEN, intersection.getTrafficLight(Direction.NORTH).getCurrentState());
            assertEquals(LightState.GREEN, intersection.getTrafficLight(Direction.SOUTH).getCurrentState());
        }

        @Test
        @DisplayName("Should detect conflict when present")
        void shouldDetectConflictWhenPresent() {
            // Manually set conflicting lights (bypassing validation for test)
            intersection.getTrafficLight(Direction.NORTH).changeState(LightState.GREEN);
            intersection.getTrafficLight(Direction.EAST).changeState(LightState.GREEN);
            
            assertTrue(intersection.hasConflict());
        }

        @Test
        @DisplayName("Should not detect conflict when none present")
        void shouldNotDetectConflictWhenNonePresent() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            intersection.changeTrafficLightState(Direction.SOUTH, LightState.GREEN, "TEST");
            
            assertFalse(intersection.hasConflict());
        }

        @Test
        @DisplayName("validateNoConflict should pass for non-conflicting state")
        void validateNoConflictShouldPassForNonConflictingState() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            
            // SOUTH doesn't conflict with NORTH
            assertDoesNotThrow(() -> intersection.validateNoConflict(Direction.SOUTH));
        }

        @Test
        @DisplayName("validateNoConflict should throw for conflicting state")
        void validateNoConflictShouldThrowForConflictingState() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            
            // EAST conflicts with NORTH
            assertThrows(IllegalStateException.class, () -> intersection.validateNoConflict(Direction.EAST));
        }
    }

    @Nested
    @DisplayName("Operation Status Tests")
    class OperationStatusTests {

        @Test
        @DisplayName("Should change operation status")
        void shouldChangeOperationStatus() {
            intersection.setOperationStatus(OperationStatus.RUNNING);
            assertEquals(OperationStatus.RUNNING, intersection.getOperationStatus());
            assertTrue(intersection.isRunning());
            assertFalse(intersection.isPaused());
        }

        @Test
        @DisplayName("Should throw exception for null operation status")
        void shouldThrowExceptionForNullOperationStatus() {
            assertThrows(NullPointerException.class, () -> intersection.setOperationStatus(null));
        }
    }

    @Nested
    @DisplayName("Emergency Stop Tests")
    class EmergencyStopTests {

        @Test
        @DisplayName("Emergency stop should set all lights to RED")
        void emergencyStopShouldSetAllLightsToRed() {
            // Set some lights to non-RED states
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            intersection.changeTrafficLightState(Direction.SOUTH, LightState.GREEN, "TEST");
            
            intersection.emergencyStop("EMERGENCY_TEST");
            
            for (Direction direction : Direction.values()) {
                assertEquals(LightState.RED, intersection.getTrafficLight(direction).getCurrentState());
            }
        }

        @Test
        @DisplayName("Emergency stop should set status to EMERGENCY")
        void emergencyStopShouldSetStatusToEmergency() {
            intersection.emergencyStop("EMERGENCY_TEST");
            assertEquals(OperationStatus.EMERGENCY, intersection.getOperationStatus());
        }
    }

    @Nested
    @DisplayName("Timing Configuration Tests")
    class TimingConfigurationTests {

        @Test
        @DisplayName("Should update green duration")
        void shouldUpdateGreenDuration() {
            intersection.setGreenDurationMs(45000);
            assertEquals(45000, intersection.getGreenDurationMs());
        }

        @Test
        @DisplayName("Should update yellow duration")
        void shouldUpdateYellowDuration() {
            intersection.setYellowDurationMs(3000);
            assertEquals(3000, intersection.getYellowDurationMs());
        }

        @Test
        @DisplayName("Should update red duration")
        void shouldUpdateRedDuration() {
            intersection.setRedDurationMs(48000);
            assertEquals(48000, intersection.getRedDurationMs());
        }

        @Test
        @DisplayName("Should throw exception for non-positive green duration")
        void shouldThrowExceptionForNonPositiveGreenDuration() {
            assertThrows(IllegalArgumentException.class, () -> intersection.setGreenDurationMs(0));
            assertThrows(IllegalArgumentException.class, () -> intersection.setGreenDurationMs(-1000));
        }

        @Test
        @DisplayName("Should throw exception for non-positive yellow duration")
        void shouldThrowExceptionForNonPositiveYellowDuration() {
            assertThrows(IllegalArgumentException.class, () -> intersection.setYellowDurationMs(0));
        }

        @Test
        @DisplayName("Should throw exception for non-positive red duration")
        void shouldThrowExceptionForNonPositiveRedDuration() {
            assertThrows(IllegalArgumentException.class, () -> intersection.setRedDurationMs(0));
        }
    }

    @Nested
    @DisplayName("History Tests")
    class HistoryTests {

        @Test
        @DisplayName("Should track state change history")
        void shouldTrackStateChangeHistory() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST1");
            intersection.changeTrafficLightState(Direction.NORTH, LightState.YELLOW, "TEST2");
            intersection.changeTrafficLightState(Direction.NORTH, LightState.RED, "TEST3");
            
            List<StateChangeEvent> history = intersection.getStateHistory();
            assertEquals(3, history.size());
        }

        @Test
        @DisplayName("Should filter history by direction")
        void shouldFilterHistoryByDirection() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            intersection.changeTrafficLightState(Direction.SOUTH, LightState.GREEN, "TEST");
            intersection.changeTrafficLightState(Direction.NORTH, LightState.YELLOW, "TEST");
            
            List<StateChangeEvent> northHistory = intersection.getStateHistoryForDirection(Direction.NORTH);
            assertEquals(2, northHistory.size());
            
            for (StateChangeEvent event : northHistory) {
                assertEquals(Direction.NORTH, event.direction());
            }
        }

        @Test
        @DisplayName("Should get recent history")
        void shouldGetRecentHistory() {
            for (int i = 0; i < 10; i++) {
                intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST" + i);
                intersection.changeTrafficLightState(Direction.NORTH, LightState.RED, "TEST" + i);
            }
            
            List<StateChangeEvent> recent = intersection.getRecentHistory(5);
            assertEquals(5, recent.size());
        }

        @Test
        @DisplayName("Should clear history")
        void shouldClearHistory() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            assertFalse(intersection.getStateHistory().isEmpty());
            
            intersection.clearHistory();
            assertTrue(intersection.getStateHistory().isEmpty());
        }

        @Test
        @DisplayName("History should be unmodifiable")
        void historyShouldBeUnmodifiable() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            List<StateChangeEvent> history = intersection.getStateHistory();
            
            assertThrows(UnsupportedOperationException.class, () -> history.clear());
        }
    }

    @Nested
    @DisplayName("State Snapshot Tests")
    class StateSnapshotTests {

        @Test
        @DisplayName("Should get current state snapshot")
        void shouldGetCurrentStateSnapshot() {
            intersection.changeTrafficLightState(Direction.NORTH, LightState.GREEN, "TEST");
            intersection.changeTrafficLightState(Direction.SOUTH, LightState.GREEN, "TEST");
            
            Map<Direction, LightState> snapshot = intersection.getCurrentStateSnapshot();
            
            assertEquals(4, snapshot.size());
            assertEquals(LightState.GREEN, snapshot.get(Direction.NORTH));
            assertEquals(LightState.GREEN, snapshot.get(Direction.SOUTH));
            assertEquals(LightState.RED, snapshot.get(Direction.EAST));
            assertEquals(LightState.RED, snapshot.get(Direction.WEST));
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Intersections with same ID should be equal")
        void intersectionsWithSameIdShouldBeEqual() {
            Intersection another = new Intersection("INT-001", "Different Name");
            assertEquals(intersection, another);
            assertEquals(intersection.hashCode(), another.hashCode());
        }

        @Test
        @DisplayName("Intersections with different IDs should not be equal")
        void intersectionsWithDifferentIdsShouldNotBeEqual() {
            Intersection another = new Intersection("INT-002", "Main Street & 1st Avenue");
            assertNotEquals(intersection, another);
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Intersection should handle concurrent state changes")
        void intersectionShouldHandleConcurrentStateChanges() throws InterruptedException {
            int threadCount = 10;
            int iterationsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < iterationsPerThread; j++) {
                            // Alternate between safe state changes
                            if (threadId % 2 == 0) {
                                try {
                                    intersection.changeTrafficLightState(Direction.NORTH, LightState.RED, "THREAD-" + threadId);
                                } catch (IllegalStateException ignored) {
                                    // Expected in concurrent scenario
                                }
                            } else {
                                try {
                                    intersection.changeTrafficLightState(Direction.EAST, LightState.RED, "THREAD-" + threadId);
                                } catch (IllegalStateException ignored) {
                                    // Expected in concurrent scenario
                                }
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));
            executor.shutdown();

            // Verify intersection is still in valid state
            assertNotNull(intersection.getOperationStatus());
            assertFalse(intersection.hasConflict());
        }
    }
}
