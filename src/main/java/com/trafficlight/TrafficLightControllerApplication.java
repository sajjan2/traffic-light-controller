package com.trafficlight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Traffic Light Controller API.
 * This application manages traffic light systems at intersections,
 * handling state changes, timing, and conflict validation.
 */
@SpringBootApplication
@EnableScheduling
public class TrafficLightControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficLightControllerApplication.class, args);
    }
}
