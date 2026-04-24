# Smart Campus Sensor & Room Management API

## 1. Overview

The Smart Campus API is a robust, production-ready RESTful web service built strictly using **JAX-RS (Jakarta RESTful Web Services)** with Jersey as the implementation. It manages the university's "Smart Campus" initiative, providing a comprehensive interface for managing Rooms, Sensors (IoT devices), and historical Sensor Readings.

### Key Features:
- **HATEOAS Discovery Endpoint** - Self-discoverable API with hypermedia links
- **Thread-Safe In-Memory Storage** - Using `ConcurrentHashMap` and `CopyOnWriteArrayList`
- **Robust Error Handling** - Custom `ExceptionMapper` classes returning structured JSON errors
- **Business Logic Constraints** - Prevents data orphans and validates foreign keys
- **Sub-Resource Locator Pattern** - Modular, maintainable API architecture
- **AOP Logging** - JAX-RS Filters for cross-cutting concerns

### Technology Stack:
- **Java 11+**
- **JAX-RS 2.1 (Jakarta EE 8)**
- **Jersey 2.34** (JAX-RS Implementation)
- **Jackson** (JSON Processing)
- **Apache Maven** (Build Tool)
- **Apache Tomcat 9** (Servlet Container)

---

## 2. Build & Run Instructions

### Prerequisites:
- Java 11 or higher
- Maven 3.6+
- Apache Tomcat 9+

### Steps:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Tharumika/Smart-Campus-API.git
   cd Smart-Campus-API
   ```

2. **Build the project:**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Deploy to Tomcat:**
   - Copy `target/SmartCampusAPI-1.0-SNAPSHOT.war` to your Tomcat `webapps` folder
   - Or run directly from NetBeans IDE

4. **Access the API:**
   ```
   http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1
   ```

---

## 3. API Endpoints

### Discovery (HATEOAS)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1` | Returns API metadata and hypermedia links |

### Rooms
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get room by ID |
| DELETE | `/api/v1/rooms/{roomId}` | Delete room (blocks if sensors exist) |

### Sensors
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sensors` | List all sensors (supports `?type=` filter) |
| POST | `/api/v1/sensors` | Register new sensor (validates roomId) |
| DELETE | `/api/v1/sensors/{sensorId}` | Delete sensor |

### Sensor Readings (Sub-Resource)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add new reading (updates currentValue) |

---

## 4. Sample cURL Commands

**1. Discovery Endpoint (HATEOAS)**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1 -H "Accept: application/json"
```

**2. Create a Room**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Library Quiet Study", "capacity": 50}'
```

**3. Get All Rooms**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms -H "Accept: application/json"
```

**4. Register a Sensor**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"type": "Temperature", "roomId": "ROOM-ID-FROM-STEP-2"}'
```

**5. Record a Sensor Reading**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/sensors/SENSOR-ID/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 22.5}'
```

---

## 5. Project Structure

```
SmartCampusAPI/
├── pom.xml                          # Maven configuration
├── README.md                       # This file
├── TECHNICAL_REPORT.md              # Technical report answers
├── src/main/java/com/smartcampus/
│   ├── config/
│   │   └── ApplicationConfig.java # JAX-RS Application setup
│   ├── exceptions/
│   │   ├── GlobalExceptionMapper.java
│   │   ├── LinkedResourceNotFoundException.java
│   │   ├── LinkedResourceNotFoundExceptionMapper.java
│   │   ├── RoomNotEmptyException.java
│   │   ├── RoomNotEmptyExceptionMapper.java
│   │   ├── SensorUnavailableException.java
│   │   └── SensorUnavailableExceptionMapper.java
│   ├── filters/
│   │   └── LoggingFilter.java        # Request/Response logging
│   ├── models/
│   │   ├── Room.java
│   │   ├── Sensor.java
│   │   └── SensorReading.java
│   ├── resources/
│   │   ├── DiscoveryResource.java
│   │   ├── RoomResource.java
│   │   ├── SensorResource.java
│   │   └── SensorReadingResource.java
│   └── store/
│       └── DataStore.java         # Thread-safe in-memory database
└── src/main/webapp/
    ├── META-INF/
    │   └── context.xml
    └── WEB-INF/
        └── web.xml                # Jersey servlet configuration
```

---

## 6. Error Responses

All errors return structured JSON (never raw Java stack traces):

| HTTP Status | Scenario |
|------------|----------|
| 400 | Bad Request (generic) |
| 403 | Sensor in MAINTENANCE/OFFLINE status |
| 404 | Resource not found |
| 409 | Delete room with active sensors |
| 422 | Invalid roomId in sensor payload |
| 500 | Unexpected server error |

---

## 7. Technical Report Questions

*Note: Full answers to all questions are provided in TECHNICAL_REPORT.md*

### Part 1.1: JAX-RS Lifecycle & Thread Safety
Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

### Part 1.2: HATEOAS & Hypermedia
Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

### Part 2.1: Returning IDs vs. Full Objects
When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

### Part 2.2: Idempotency of the DELETE Operation
Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

### Part 3.1: @Consumes and Media Type Mismatches
We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

### Part 3.2: Query Parameters vs. Path Parameters
You implemented this filtering using @QueryParam. Contrast this with an alternate design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the queryparameterapproach generally considered superior for filtering and searching collections?

### Part 4.1: Sub-Resource Locator Pattern
Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?

### Part 5.2: 422 Unprocessable Entity vs. 404 Not Found
Why is HTTP422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

### Part 5.4: Cybersecurity Risks of Stack Traces
From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

### Part 5.5: JAX-RS Filters for Cross-Cutting Concerns
Why is it advantageous to use JAX-RS filters for logging, rather than manually inserting Logger statements inside every single resource method?