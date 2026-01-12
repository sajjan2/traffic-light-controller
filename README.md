# Traffic Light Controller API

A RESTful API for controlling traffic light systems at intersections, built with Java Spring Boot.

## Features

- **Multi-Intersection Support**: Manage multiple intersections simultaneously
- **State Management**: Control traffic light states (RED, YELLOW, GREEN) for all directions
- **Conflict Validation**: Prevents conflicting directions from being green simultaneously
- **Automatic Cycling**: Automatic traffic light sequence cycling when intersection is running
- **Operation Control**: Start, pause, resume, and emergency stop operations
- **Timing Configuration**: Configurable timing for each light phase
- **History Tracking**: Complete state change history with timestamps
- **Thread-Safe**: Designed for concurrent access with proper synchronization
- **RESTful API**: Clean REST API with OpenAPI/Swagger documentation

## Technology Stack

- Java 17
- Spring Boot 3.2.1
- Spring Web (RESTful services)
- Spring Validation
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5 & Mockito (Testing)
- Maven

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Building the Project

```bash
cd traffic-light-controller
mvn clean install
```

### Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Running Tests

```bash
mvn test
```

## API Documentation

Once the application is running, access the Swagger UI at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

## API Endpoints

### Intersection Management

| Method | Endpoint                     | Description               |
| ------ | ---------------------------- | ------------------------- |
| POST   | `/api/v1/intersections`      | Create a new intersection |
| GET    | `/api/v1/intersections`      | Get all intersections     |
| GET    | `/api/v1/intersections/{id}` | Get intersection by ID    |
| DELETE | `/api/v1/intersections/{id}` | Delete an intersection    |

### Traffic Light Control

| Method | Endpoint                                        | Description                |
| ------ | ----------------------------------------------- | -------------------------- |
| PUT    | `/api/v1/intersections/{id}/lights`             | Change traffic light state |
| GET    | `/api/v1/intersections/{id}/lights/{direction}` | Get traffic light state    |

### Operation Control

| Method | Endpoint                                    | Description               |
| ------ | ------------------------------------------- | ------------------------- |
| POST   | `/api/v1/intersections/{id}/start`          | Start automatic operation |
| POST   | `/api/v1/intersections/{id}/pause`          | Pause operation           |
| POST   | `/api/v1/intersections/{id}/resume`         | Resume operation          |
| POST   | `/api/v1/intersections/{id}/emergency-stop` | Emergency stop (all RED)  |

### Configuration

| Method | Endpoint                            | Description                 |
| ------ | ----------------------------------- | --------------------------- |
| PUT    | `/api/v1/intersections/{id}/timing` | Update timing configuration |

### History

| Method | Endpoint                                                   | Description               |
| ------ | ---------------------------------------------------------- | ------------------------- |
| GET    | `/api/v1/intersections/{id}/history`                       | Get complete history      |
| GET    | `/api/v1/intersections/{id}/history/direction/{direction}` | Get history for direction |
| GET    | `/api/v1/intersections/{id}/history/recent?count=10`       | Get recent history        |
| DELETE | `/api/v1/intersections/{id}/history`                       | Clear history             |

### Phase Information

| Method | Endpoint                           | Description            |
| ------ | ---------------------------------- | ---------------------- |
| GET    | `/api/v1/intersections/{id}/phase` | Get current phase info |

## Example Usage

### Create an Intersection

```bash
curl -X POST http://localhost:8080/api/v1/intersections \
  -H "Content-Type: application/json" \
  -d '{
    "id": "INT-001",
    "name": "Main Street & 1st Avenue",
    "timingConfig": {
      "greenDurationMs": 30000,
      "yellowDurationMs": 5000,
      "redDurationMs": 35000
    }
  }'
```

### Start the Intersection

```bash
curl -X POST http://localhost:8080/api/v1/intersections/INT-001/start
```

### Change Traffic Light State

```bash
curl -X PUT http://localhost:8080/api/v1/intersections/INT-001/lights \
  -H "Content-Type: application/json" \
  -d '{
    "direction": "NORTH",
    "newState": "GREEN"
  }'
```

### Get Current State

```bash
curl http://localhost:8080/api/v1/intersections/INT-001
```

### Emergency Stop

```bash
curl -X POST http://localhost:8080/api/v1/intersections/INT-001/emergency-stop
```

## Architecture

### Domain Model

```
Intersection
├── id: String
├── name: String
├── operationStatus: OperationStatus (RUNNING, PAUSED, EMERGENCY, MAINTENANCE)
├── trafficLights: Map<Direction, TrafficLight>
├── stateHistory: List<StateChangeEvent>
└── timingConfig: (greenDurationMs, yellowDurationMs, redDurationMs)

TrafficLight
├── direction: Direction (NORTH, SOUTH, EAST, WEST)
├── currentState: LightState (RED, YELLOW, GREEN)
├── previousState: LightState
└── lastStateChangeTime: Instant

Direction
├── NORTH (conflicts with EAST, WEST)
├── SOUTH (conflicts with EAST, WEST)
├── EAST (conflicts with NORTH, SOUTH)
└── WEST (conflicts with NORTH, SOUTH)
```

### Traffic Light Phases

The automatic cycling follows this sequence:

1. **NORTH_SOUTH_GREEN**: N/S green, E/W red
2. **NORTH_SOUTH_YELLOW**: N/S yellow, E/W red
3. **EAST_WEST_GREEN**: E/W green, N/S red
4. **EAST_WEST_YELLOW**: E/W yellow, N/S red
5. (Repeat)

### Conflict Rules

- NORTH and SOUTH can be green together (parallel)
- EAST and WEST can be green together (parallel)
- NORTH/SOUTH cannot be green when EAST/WEST is green (conflict)

## Design Decisions

### Thread Safety

- `ConcurrentHashMap` for intersection storage
- `AtomicReference` for state management in TrafficLight
- `ReentrantReadWriteLock` for history operations
- `CopyOnWriteArrayList` for thread-safe history iteration

### Clean Code Principles

- Single Responsibility: Each class has one clear purpose
- Open/Closed: Easy to extend with new features
- Interface Segregation: Service interface defines clear contract
- Dependency Injection: Spring manages dependencies

### Edge Cases Handled

- Duplicate intersection creation
- Non-existent intersection access
- Conflicting light state changes
- Invalid timing configurations
- Concurrent state modifications
- History size limits (max 1000 events)

## Future Enhancements

- Database persistence (JPA/Hibernate)
- WebSocket support for real-time updates
- Pedestrian crossing signals
- Turn signals (left/right arrows)
- Sensor integration for adaptive timing
- Multi-intersection coordination
- Traffic flow analytics
- Authentication and authorization

## License

This project is licensed under the MIT License.
