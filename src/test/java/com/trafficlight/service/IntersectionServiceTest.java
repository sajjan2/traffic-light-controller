package com.trafficlight.service;

import com.trafficlight.dto.*;
import com.trafficlight.exception.IntersectionAlreadyExistsException;
import com.trafficlight.exception.IntersectionNotFoundException;
import com.trafficlight.exception.InvalidOperationException;
import com.trafficlight.model.Direction;
import com.trafficlight.model.LightState;
import com.trafficlight.model.OperationStatus;
import com.trafficlight.service.impl.IntersectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the IntersectionService.
 */
@DisplayName("IntersectionService Tests")
class IntersectionServiceTest {

    private IntersectionService intersectionService;

    @BeforeEach
    void setUp() {
        intersectionService = new IntersectionServiceImpl();
    }

    @Nested
    @DisplayName("Create Intersection Tests")
    class CreateIntersectionTests {

        @Test
        @DisplayName("Should create intersection successfully")
        void shouldCreateIntersectionSuccessfully() {
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    "INT-001", "Main Street", null);
            
            IntersectionDto result = intersectionService.createIntersection(request);
            
            assertNotNull(result);
            assertEquals("INT-001", result.id());
            assertEquals("Main Street", result.name());
            assertEquals(OperationStatus.PAUSED, result.operationStatus());
        }

        @Test
        @DisplayName("Should create intersection with custom timing")
        void shouldCreateIntersectionWithCustomTiming() {
            TimingConfigDto timing = new TimingConfigDto(20000L, 3000L, 23000L);
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    "INT-001", "Main Street", timing);
            
            IntersectionDto result = intersectionService.createIntersection(request);
            
            assertEquals(20000L, result.timingConfig().greenDurationMs());
            assertEquals(3000L, result.timingConfig().yellowDurationMs());
            assertEquals(23000L, result.timingConfig().redDurationMs());
        }

        @Test
        @DisplayName("Should throw exception for duplicate intersection ID")
        void shouldThrowExceptionForDuplicateId() {
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    "INT-001", "Main Street", null);
            
            intersectionService.createIntersection(request);
            
            assertThrows(IntersectionAlreadyExistsException.class, () ->
                    intersectionService.createIntersection(request));
        }

        @Test
        @DisplayName("All traffic lights should start RED")
        void allTrafficLightsShouldStartRed() {
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    "INT-001", "Main Street", null);
            
            IntersectionDto result = intersectionService.createIntersection(request);
            
            for (Direction direction : Direction.values()) {
                assertEquals(LightState.RED, result.trafficLights().get(direction).currentState());
            }
        }
    }

    @Nested
    @DisplayName("Get Intersection Tests")
    class GetIntersectionTests {

        @Test
        @DisplayName("Should get intersection by ID")
        void shouldGetIntersectionById() {
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    "INT-001", "Main Street", null);
            intersectionService.createIntersection(request);
            
            IntersectionDto result = intersectionService.getIntersection("INT-001");
            
            assertNotNull(result);
            assertEquals("INT-001", result.id());
        }

        @Test
        @DisplayName("Should throw exception for non-existent intersection")
        void shouldThrowExceptionForNonExistentIntersection() {
            assertThrows(IntersectionNotFoundException.class, () ->
                    intersectionService.getIntersection("NON-EXISTENT"));
        }

        @Test
        @DisplayName("Should get all intersections")
        void shouldGetAllIntersections() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Street 1", null));
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-002", "Street 2", null));
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-003", "Street 3", null));
            
            List<IntersectionDto> results = intersectionService.getAllIntersections();
            
            assertEquals(3, results.size());
        }
    }

    @Nested
    @DisplayName("Delete Intersection Tests")
    class DeleteIntersectionTests {

        @Test
        @DisplayName("Should delete intersection")
        void shouldDeleteIntersection() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Main Street", null));
            
            intersectionService.deleteIntersection("INT-001");
            
            assertThrows(IntersectionNotFoundException.class, () ->
                    intersectionService.getIntersection("INT-001"));
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent intersection")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            assertThrows(IntersectionNotFoundException.class, () ->
                    intersectionService.deleteIntersection("NON-EXISTENT"));
        }
    }

    @Nested
    @DisplayName("Change Traffic Light State Tests")
    class ChangeTrafficLightStateTests {

        @BeforeEach
        void createIntersection() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Main Street", null));
        }

        @Test
        @DisplayName("Should change traffic light state")
        void shouldChangeTrafficLightState() {
            ChangeLightStateRequest request = new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN);
            
            IntersectionDto result = intersectionService.changeTrafficLightState("INT-001", request);
            
            assertEquals(LightState.GREEN, result.trafficLights().get(Direction.NORTH).currentState());
        }

        @Test
        @DisplayName("Should throw exception for conflicting state change")
        void shouldThrowExceptionForConflictingStateChange() {
            // Set NORTH to GREEN
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN));
            
            // Try to set EAST to GREEN (conflicts)
            assertThrows(IllegalStateException.class, () ->
                    intersectionService.changeTrafficLightState("INT-001",
                            new ChangeLightStateRequest(Direction.EAST, LightState.GREEN)));
        }

        @Test
        @DisplayName("Should allow parallel directions to be GREEN")
        void shouldAllowParallelDirectionsToBeGreen() {
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN));
            
            IntersectionDto result = intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.SOUTH, LightState.GREEN));
            
            assertEquals(LightState.GREEN, result.trafficLights().get(Direction.NORTH).currentState());
            assertEquals(LightState.GREEN, result.trafficLights().get(Direction.SOUTH).currentState());
        }
    }

    @Nested
    @DisplayName("Operation Control Tests")
    class OperationControlTests {

        @BeforeEach
        void createIntersection() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Main Street", null));
        }

        @Test
        @DisplayName("Should start intersection")
        void shouldStartIntersection() {
            IntersectionDto result = intersectionService.startIntersection("INT-001");
            
            assertEquals(OperationStatus.RUNNING, result.operationStatus());
        }

        @Test
        @DisplayName("Should throw exception when starting already running intersection")
        void shouldThrowExceptionWhenStartingAlreadyRunning() {
            intersectionService.startIntersection("INT-001");
            
            assertThrows(InvalidOperationException.class, () ->
                    intersectionService.startIntersection("INT-001"));
        }

        @Test
        @DisplayName("Should pause intersection")
        void shouldPauseIntersection() {
            intersectionService.startIntersection("INT-001");
            
            IntersectionDto result = intersectionService.pauseIntersection("INT-001");
            
            assertEquals(OperationStatus.PAUSED, result.operationStatus());
        }

        @Test
        @DisplayName("Should throw exception when pausing non-running intersection")
        void shouldThrowExceptionWhenPausingNonRunning() {
            assertThrows(InvalidOperationException.class, () ->
                    intersectionService.pauseIntersection("INT-001"));
        }

        @Test
        @DisplayName("Should resume intersection")
        void shouldResumeIntersection() {
            intersectionService.startIntersection("INT-001");
            intersectionService.pauseIntersection("INT-001");
            
            IntersectionDto result = intersectionService.resumeIntersection("INT-001");
            
            assertEquals(OperationStatus.RUNNING, result.operationStatus());
        }

        @Test
        @DisplayName("Should throw exception when resuming non-paused intersection")
        void shouldThrowExceptionWhenResumingNonPaused() {
            intersectionService.startIntersection("INT-001");
            
            assertThrows(InvalidOperationException.class, () ->
                    intersectionService.resumeIntersection("INT-001"));
        }

        @Test
        @DisplayName("Should execute emergency stop")
        void shouldExecuteEmergencyStop() {
            intersectionService.startIntersection("INT-001");
            
            IntersectionDto result = intersectionService.emergencyStop("INT-001");
            
            assertEquals(OperationStatus.EMERGENCY, result.operationStatus());
            for (Direction direction : Direction.values()) {
                assertEquals(LightState.RED, result.trafficLights().get(direction).currentState());
            }
        }
    }

    @Nested
    @DisplayName("Timing Configuration Tests")
    class TimingConfigurationTests {

        @BeforeEach
        void createIntersection() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Main Street", null));
        }

        @Test
        @DisplayName("Should update timing configuration")
        void shouldUpdateTimingConfiguration() {
            TimingConfigDto newTiming = new TimingConfigDto(25000L, 4000L, 29000L);
            
            IntersectionDto result = intersectionService.updateTimingConfig("INT-001", newTiming);
            
            assertEquals(25000L, result.timingConfig().greenDurationMs());
            assertEquals(4000L, result.timingConfig().yellowDurationMs());
            assertEquals(29000L, result.timingConfig().redDurationMs());
        }
    }

    @Nested
    @DisplayName("History Tests")
    class HistoryTests {

        @BeforeEach
        void createIntersection() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Main Street", null));
        }

        @Test
        @DisplayName("Should get state history")
        void shouldGetStateHistory() {
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN));
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.NORTH, LightState.YELLOW));
            
            List<StateChangeEventDto> history = intersectionService.getStateHistory("INT-001");
            
            assertEquals(2, history.size());
        }

        @Test
        @DisplayName("Should get history for specific direction")
        void shouldGetHistoryForSpecificDirection() {
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN));
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.SOUTH, LightState.GREEN));
            
            List<StateChangeEventDto> northHistory = 
                    intersectionService.getStateHistoryForDirection("INT-001", Direction.NORTH);
            
            assertEquals(1, northHistory.size());
            assertEquals(Direction.NORTH, northHistory.get(0).direction());
        }

        @Test
        @DisplayName("Should get recent history")
        void shouldGetRecentHistory() {
            for (int i = 0; i < 10; i++) {
                intersectionService.changeTrafficLightState("INT-001",
                        new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN));
                intersectionService.changeTrafficLightState("INT-001",
                        new ChangeLightStateRequest(Direction.NORTH, LightState.RED));
            }
            
            List<StateChangeEventDto> recent = intersectionService.getRecentHistory("INT-001", 5);
            
            assertEquals(5, recent.size());
        }

        @Test
        @DisplayName("Should clear history")
        void shouldClearHistory() {
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN));
            
            intersectionService.clearHistory("INT-001");
            
            List<StateChangeEventDto> history = intersectionService.getStateHistory("INT-001");
            assertTrue(history.isEmpty());
        }
    }

    @Nested
    @DisplayName("Traffic Light State Query Tests")
    class TrafficLightStateQueryTests {

        @BeforeEach
        void createIntersection() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Main Street", null));
        }

        @Test
        @DisplayName("Should get traffic light state")
        void shouldGetTrafficLightState() {
            intersectionService.changeTrafficLightState("INT-001",
                    new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN));
            
            TrafficLightStateDto state = intersectionService.getTrafficLightState("INT-001", Direction.NORTH);
            
            assertEquals(Direction.NORTH, state.direction());
            assertEquals(LightState.GREEN, state.currentState());
        }
    }

    @Nested
    @DisplayName("Start Intersection Safe State Tests")
    class StartIntersectionSafeStateTests {

        @Test
        @DisplayName("Starting intersection should initialize with safe state")
        void startingIntersectionShouldInitializeWithSafeState() {
            intersectionService.createIntersection(new CreateIntersectionRequest("INT-001", "Main Street", null));
            
            IntersectionDto result = intersectionService.startIntersection("INT-001");
            
            // NORTH and SOUTH should be GREEN
            assertEquals(LightState.GREEN, result.trafficLights().get(Direction.NORTH).currentState());
            assertEquals(LightState.GREEN, result.trafficLights().get(Direction.SOUTH).currentState());
            
            // EAST and WEST should be RED
            assertEquals(LightState.RED, result.trafficLights().get(Direction.EAST).currentState());
            assertEquals(LightState.RED, result.trafficLights().get(Direction.WEST).currentState());
            
            // No conflicts
            assertFalse(result.hasConflict());
        }
    }
}
