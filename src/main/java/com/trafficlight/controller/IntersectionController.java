package com.trafficlight.controller;

import com.trafficlight.dto.*;
import com.trafficlight.model.Direction;
import com.trafficlight.service.IntersectionService;
import com.trafficlight.service.TrafficLightScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing traffic light intersections.
 * Provides endpoints for CRUD operations, state management, and history retrieval.
 */
@RestController
@RequestMapping("/api/v1/intersections")
@Tag(name = "Intersection Controller", description = "APIs for managing traffic light intersections")
public class IntersectionController {

    private static final Logger logger = LoggerFactory.getLogger(IntersectionController.class);

    private final IntersectionService intersectionService;
    private final TrafficLightScheduler trafficLightScheduler;

    public IntersectionController(IntersectionService intersectionService, 
                                  TrafficLightScheduler trafficLightScheduler) {
        this.intersectionService = intersectionService;
        this.trafficLightScheduler = trafficLightScheduler;
    }

    // ==================== CRUD Operations ====================

    @PostMapping
    @Operation(summary = "Create a new intersection", 
               description = "Creates a new traffic light intersection with the specified configuration")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Intersection created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Intersection already exists")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> createIntersection(
            @Valid @RequestBody CreateIntersectionRequest request) {
        logger.info("REST: Creating intersection with ID: {}", request.id());
        IntersectionDto intersection = intersectionService.createIntersection(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Intersection created successfully", intersection));
    }

    @GetMapping("/{intersectionId}")
    @Operation(summary = "Get intersection by ID", 
               description = "Retrieves the current state of an intersection")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Intersection found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> getIntersection(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        IntersectionDto intersection = intersectionService.getIntersection(intersectionId);
        return ResponseEntity.ok(ApiResponse.success(intersection));
    }

    @GetMapping
    @Operation(summary = "Get all intersections", 
               description = "Retrieves all registered intersections")
    public ResponseEntity<ApiResponse<List<IntersectionDto>>> getAllIntersections() {
        List<IntersectionDto> intersections = intersectionService.getAllIntersections();
        return ResponseEntity.ok(ApiResponse.success(intersections));
    }

    @DeleteMapping("/{intersectionId}")
    @Operation(summary = "Delete an intersection", 
               description = "Removes an intersection from the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Intersection deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteIntersection(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        logger.info("REST: Deleting intersection: {}", intersectionId);
        intersectionService.deleteIntersection(intersectionId);
        trafficLightScheduler.resetPhase(intersectionId);
        return ResponseEntity.ok(ApiResponse.success("Intersection deleted successfully", null));
    }

    // ==================== Traffic Light State Operations ====================

    @PutMapping("/{intersectionId}/lights")
    @Operation(summary = "Change traffic light state", 
               description = "Changes the state of a specific traffic light at an intersection")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "State changed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "State change would cause conflict")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> changeTrafficLightState(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId,
            @Valid @RequestBody ChangeLightStateRequest request) {
        logger.info("REST: Changing light state at {}: {} -> {}", 
                intersectionId, request.direction(), request.newState());
        IntersectionDto intersection = intersectionService.changeTrafficLightState(intersectionId, request);
        return ResponseEntity.ok(ApiResponse.success("Traffic light state changed", intersection));
    }

    @GetMapping("/{intersectionId}/lights/{direction}")
    @Operation(summary = "Get traffic light state", 
               description = "Gets the current state of a specific traffic light")
    public ResponseEntity<ApiResponse<TrafficLightStateDto>> getTrafficLightState(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId,
            @Parameter(description = "Direction") @PathVariable Direction direction) {
        TrafficLightStateDto state = intersectionService.getTrafficLightState(intersectionId, direction);
        return ResponseEntity.ok(ApiResponse.success(state));
    }

    // ==================== Operation Control ====================

    @PostMapping("/{intersectionId}/start")
    @Operation(summary = "Start intersection operation", 
               description = "Starts automatic traffic light cycling at the intersection")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Intersection started"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Intersection already running"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> startIntersection(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        logger.info("REST: Starting intersection: {}", intersectionId);
        IntersectionDto intersection = intersectionService.startIntersection(intersectionId);
        return ResponseEntity.ok(ApiResponse.success("Intersection started", intersection));
    }

    @PostMapping("/{intersectionId}/pause")
    @Operation(summary = "Pause intersection operation", 
               description = "Pauses automatic traffic light cycling, maintaining current states")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Intersection paused"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Intersection not running"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> pauseIntersection(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        logger.info("REST: Pausing intersection: {}", intersectionId);
        IntersectionDto intersection = intersectionService.pauseIntersection(intersectionId);
        return ResponseEntity.ok(ApiResponse.success("Intersection paused", intersection));
    }

    @PostMapping("/{intersectionId}/resume")
    @Operation(summary = "Resume intersection operation", 
               description = "Resumes automatic traffic light cycling from paused state")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Intersection resumed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Intersection not paused"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> resumeIntersection(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        logger.info("REST: Resuming intersection: {}", intersectionId);
        IntersectionDto intersection = intersectionService.resumeIntersection(intersectionId);
        return ResponseEntity.ok(ApiResponse.success("Intersection resumed", intersection));
    }

    @PostMapping("/{intersectionId}/emergency-stop")
    @Operation(summary = "Emergency stop", 
               description = "Sets all lights to RED and stops operation immediately")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Emergency stop executed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> emergencyStop(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        logger.warn("REST: Emergency stop at intersection: {}", intersectionId);
        IntersectionDto intersection = intersectionService.emergencyStop(intersectionId);
        trafficLightScheduler.resetPhase(intersectionId);
        return ResponseEntity.ok(ApiResponse.success("Emergency stop executed - all lights RED", intersection));
    }

    // ==================== Configuration ====================

    @PutMapping("/{intersectionId}/timing")
    @Operation(summary = "Update timing configuration", 
               description = "Updates the timing configuration for traffic light phases")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Timing updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid timing values"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Intersection not found")
    })
    public ResponseEntity<ApiResponse<IntersectionDto>> updateTimingConfig(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId,
            @Valid @RequestBody TimingConfigDto timingConfig) {
        logger.info("REST: Updating timing config for intersection: {}", intersectionId);
        IntersectionDto intersection = intersectionService.updateTimingConfig(intersectionId, timingConfig);
        return ResponseEntity.ok(ApiResponse.success("Timing configuration updated", intersection));
    }

    // ==================== History ====================

    @GetMapping("/{intersectionId}/history")
    @Operation(summary = "Get state change history", 
               description = "Retrieves the complete state change history for an intersection")
    public ResponseEntity<ApiResponse<List<StateChangeEventDto>>> getStateHistory(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        List<StateChangeEventDto> history = intersectionService.getStateHistory(intersectionId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/{intersectionId}/history/direction/{direction}")
    @Operation(summary = "Get history for direction", 
               description = "Retrieves state change history for a specific direction")
    public ResponseEntity<ApiResponse<List<StateChangeEventDto>>> getHistoryForDirection(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId,
            @Parameter(description = "Direction") @PathVariable Direction direction) {
        List<StateChangeEventDto> history = intersectionService.getStateHistoryForDirection(intersectionId, direction);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/{intersectionId}/history/recent")
    @Operation(summary = "Get recent history", 
               description = "Retrieves the most recent state change events")
    public ResponseEntity<ApiResponse<List<StateChangeEventDto>>> getRecentHistory(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId,
            @Parameter(description = "Number of events to retrieve") @RequestParam(defaultValue = "10") int count) {
        List<StateChangeEventDto> history = intersectionService.getRecentHistory(intersectionId, count);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @DeleteMapping("/{intersectionId}/history")
    @Operation(summary = "Clear history", 
               description = "Clears all state change history for an intersection")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        logger.info("REST: Clearing history for intersection: {}", intersectionId);
        intersectionService.clearHistory(intersectionId);
        return ResponseEntity.ok(ApiResponse.success("History cleared", null));
    }

    // ==================== Scheduler Info ====================

    @GetMapping("/{intersectionId}/phase")
    @Operation(summary = "Get current phase info", 
               description = "Gets information about the current traffic phase and timing")
    public ResponseEntity<ApiResponse<PhaseInfoDto>> getCurrentPhaseInfo(
            @Parameter(description = "Intersection ID") @PathVariable String intersectionId) {
        TrafficLightScheduler.TrafficPhase phase = trafficLightScheduler.getCurrentPhase(intersectionId);
        long timeRemaining = trafficLightScheduler.getTimeRemainingInPhase(intersectionId);
        
        PhaseInfoDto phaseInfo = new PhaseInfoDto(
                phase != null ? phase.name() : "NOT_RUNNING",
                timeRemaining
        );
        
        return ResponseEntity.ok(ApiResponse.success(phaseInfo));
    }

    /**
     * DTO for phase information.
     */
    public record PhaseInfoDto(String currentPhase, long timeRemainingMs) {}
}
