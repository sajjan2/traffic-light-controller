package com.trafficlight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trafficlight.dto.ChangeLightStateRequest;
import com.trafficlight.dto.CreateIntersectionRequest;
import com.trafficlight.dto.TimingConfigDto;
import com.trafficlight.model.Direction;
import com.trafficlight.model.LightState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the IntersectionController.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("IntersectionController Integration Tests")
class IntersectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/intersections";
    private int intersectionCounter = 0;

    private String createUniqueIntersectionId() {
        return "INT-TEST-" + System.currentTimeMillis() + "-" + (++intersectionCounter);
    }

    @Nested
    @DisplayName("Create Intersection Tests")
    class CreateIntersectionTests {

        @Test
        @DisplayName("Should create intersection successfully")
        void shouldCreateIntersectionSuccessfully() throws Exception {
            String id = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    id, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.name").value("Test Street"))
                    .andExpect(jsonPath("$.data.operationStatus").value("PAUSED"));
        }

        @Test
        @DisplayName("Should create intersection with custom timing")
        void shouldCreateIntersectionWithCustomTiming() throws Exception {
            String id = createUniqueIntersectionId();
            TimingConfigDto timing = new TimingConfigDto(20000L, 3000L, 23000L);
            CreateIntersectionRequest request = new CreateIntersectionRequest(id, "Test Street", timing);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.timingConfig.greenDurationMs").value(20000))
                    .andExpect(jsonPath("$.data.timingConfig.yellowDurationMs").value(3000))
                    .andExpect(jsonPath("$.data.timingConfig.redDurationMs").value(23000));
        }

        @Test
        @DisplayName("Should return 409 for duplicate intersection")
        void shouldReturn409ForDuplicateIntersection() throws Exception {
            String id = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(id, "Test Street", null);

            // Create first
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Try to create duplicate
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            CreateIntersectionRequest request = new CreateIntersectionRequest("", "", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Intersection Tests")
    class GetIntersectionTests {

        @Test
        @DisplayName("Should get intersection by ID")
        void shouldGetIntersectionById() throws Exception {
            String id = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(id, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(BASE_URL + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(id));
        }

        @Test
        @DisplayName("Should return 404 for non-existent intersection")
        void shouldReturn404ForNonExistentIntersection() throws Exception {
            mockMvc.perform(get(BASE_URL + "/NON-EXISTENT"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should get all intersections")
        void shouldGetAllIntersections() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("Delete Intersection Tests")
    class DeleteIntersectionTests {

        @Test
        @DisplayName("Should delete intersection")
        void shouldDeleteIntersection() throws Exception {
            String id = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(id, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete(BASE_URL + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get(BASE_URL + "/" + id))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Traffic Light State Tests")
    class TrafficLightStateTests {

        private String intersectionId;

        @BeforeEach
        void createIntersection() throws Exception {
            intersectionId = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    intersectionId, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should change traffic light state")
        void shouldChangeTrafficLightState() throws Exception {
            ChangeLightStateRequest request = new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN);

            mockMvc.perform(put(BASE_URL + "/" + intersectionId + "/lights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trafficLights.NORTH.currentState").value("GREEN"));
        }

        @Test
        @DisplayName("Should return 409 for conflicting state change")
        void shouldReturn409ForConflictingStateChange() throws Exception {
            // Set NORTH to GREEN
            ChangeLightStateRequest request1 = new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN);
            mockMvc.perform(put(BASE_URL + "/" + intersectionId + "/lights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isOk());

            // Try to set EAST to GREEN (conflicts)
            ChangeLightStateRequest request2 = new ChangeLightStateRequest(Direction.EAST, LightState.GREEN);
            mockMvc.perform(put(BASE_URL + "/" + intersectionId + "/lights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should get traffic light state")
        void shouldGetTrafficLightState() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + intersectionId + "/lights/NORTH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.direction").value("NORTH"))
                    .andExpect(jsonPath("$.data.currentState").value("RED"));
        }
    }

    @Nested
    @DisplayName("Operation Control Tests")
    class OperationControlTests {

        private String intersectionId;

        @BeforeEach
        void createIntersection() throws Exception {
            intersectionId = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    intersectionId, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should start intersection")
        void shouldStartIntersection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/start"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.operationStatus").value("RUNNING"));
        }

        @Test
        @DisplayName("Should pause intersection")
        void shouldPauseIntersection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/start"))
                    .andExpect(status().isOk());

            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/pause"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.operationStatus").value("PAUSED"));
        }

        @Test
        @DisplayName("Should resume intersection")
        void shouldResumeIntersection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/start"))
                    .andExpect(status().isOk());

            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/pause"))
                    .andExpect(status().isOk());

            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/resume"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.operationStatus").value("RUNNING"));
        }

        @Test
        @DisplayName("Should execute emergency stop")
        void shouldExecuteEmergencyStop() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/start"))
                    .andExpect(status().isOk());

            mockMvc.perform(post(BASE_URL + "/" + intersectionId + "/emergency-stop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.operationStatus").value("EMERGENCY"))
                    .andExpect(jsonPath("$.data.trafficLights.NORTH.currentState").value("RED"))
                    .andExpect(jsonPath("$.data.trafficLights.SOUTH.currentState").value("RED"))
                    .andExpect(jsonPath("$.data.trafficLights.EAST.currentState").value("RED"))
                    .andExpect(jsonPath("$.data.trafficLights.WEST.currentState").value("RED"));
        }
    }

    @Nested
    @DisplayName("Timing Configuration Tests")
    class TimingConfigurationTests {

        private String intersectionId;

        @BeforeEach
        void createIntersection() throws Exception {
            intersectionId = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    intersectionId, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should update timing configuration")
        void shouldUpdateTimingConfiguration() throws Exception {
            TimingConfigDto timing = new TimingConfigDto(25000L, 4000L, 29000L);

            mockMvc.perform(put(BASE_URL + "/" + intersectionId + "/timing")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(timing)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.timingConfig.greenDurationMs").value(25000))
                    .andExpect(jsonPath("$.data.timingConfig.yellowDurationMs").value(4000))
                    .andExpect(jsonPath("$.data.timingConfig.redDurationMs").value(29000));
        }
    }

    @Nested
    @DisplayName("History Tests")
    class HistoryTests {

        private String intersectionId;

        @BeforeEach
        void createIntersection() throws Exception {
            intersectionId = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    intersectionId, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should get state history")
        void shouldGetStateHistory() throws Exception {
            // Make some state changes
            ChangeLightStateRequest request = new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN);
            mockMvc.perform(put(BASE_URL + "/" + intersectionId + "/lights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/" + intersectionId + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should get history for direction")
        void shouldGetHistoryForDirection() throws Exception {
            ChangeLightStateRequest request = new ChangeLightStateRequest(Direction.NORTH, LightState.GREEN);
            mockMvc.perform(put(BASE_URL + "/" + intersectionId + "/lights")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE_URL + "/" + intersectionId + "/history/direction/NORTH"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should get recent history")
        void shouldGetRecentHistory() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + intersectionId + "/history/recent")
                            .param("count", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("Should clear history")
        void shouldClearHistory() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + intersectionId + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Phase Info Tests")
    class PhaseInfoTests {

        private String intersectionId;

        @BeforeEach
        void createIntersection() throws Exception {
            intersectionId = createUniqueIntersectionId();
            CreateIntersectionRequest request = new CreateIntersectionRequest(
                    intersectionId, "Test Street", null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should get phase info")
        void shouldGetPhaseInfo() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + intersectionId + "/phase"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentPhase").exists())
                    .andExpect(jsonPath("$.data.timeRemainingMs").exists());
        }
    }
}
