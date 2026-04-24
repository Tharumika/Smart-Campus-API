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