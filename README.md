# Smart Campus Sensor & Room Management API

## University of Westminster - Client-Server Architectures (5COSC022W)
**Module Leader:** Dr. Hamed Hamzeh  
**Coursework Weight:** 60% of final grade

---

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
   git clone <your-github-repo-url>
   cd SmartCampusAPI
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
curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1 \
  -H "Accept: application/json"
```

**2. Create a Room**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"name": "Library Quiet Study", "capacity": 50}'
```

**3. Get All Rooms**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms \
  -H "Accept: application/json"
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

## 5. Technical Report (Coursework Questions)

### Part 1.1: JAX-RS Lifecycle & Thread Safety

**Question:** Explain the default lifecycle of a JAX-RS Resource class. How does this architectural decision impact the way you manage in-memory data structures?

**Answer:**  
By default, the JAX-RS runtime uses a **Request-Scoped** lifecycle. A brand new instance of a Resource class (e.g., `RoomResource`) is instantiated for **every single incoming HTTP request**, and then garbage collected after the response is sent. This means that if 100 sensors send data simultaneously, 100 separate Java threads create 100 different `RoomResource` instances.

To prevent race conditions and data loss, the in-memory data structures must be **decoupled from the Resource lifecycle**. I implemented a synchronized Singleton `DataStore` using `ConcurrentHashMap` and `CopyOnWriteArrayList`. These concurrent collections use fine-grained locking (bucket-level locks), ensuring thread-safe reads and writes across simultaneous API calls without blocking the entire data store.

---

### Part 1.2: HATEOAS & Hypermedia

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design?

**Answer:**  
Hypermedia as the Engine of Application State (HATEOAS) allows an API to return navigational links (`_links`) alongside data. This is a hallmark of advanced RESTful design because it makes the API **self-discoverable**. Client developers do not need to hardcode URL paths or rely on static, potentially outdated documentation. Instead, the client dynamically follows the URIs provided by the server, drastically **reducing tight coupling** between the frontend and backend architectures. If the server's routing changes, clients automatically adapt without code updates.

---

### Part 2.1: Returning IDs vs. Full Objects

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning full room objects?

**Answer:**  
Returning only a list of IDs (e.g., `["LIB-301", "LIB-302"]`) minimizes network bandwidth and payload overhead. However, it forces the client to suffer the **"N+1 query problem"**, requiring subsequent HTTP GET requests for every single ID to render a UI. Conversely, returning full objects increases payload size and serialization time, but allows the client to process and render data immediately in a **single network round-trip**. The choice depends on specific UX requirements and mobile network constraints.

---

### Part 2.2: Idempotency of the DELETE Operation

**Question:** Is the DELETE operation idempotent? Provide a detailed justification.

**Answer:**  
Yes, the `DELETE` operation in this implementation is **strictly idempotent**. Idempotency means that making multiple identical requests has the exact same effect on the server's state as making a single request. If a client successfully deletes `Room A`, it returns `204 No Content`. The server state is modified (the room is gone). If the client sends the exact same `DELETE` request 5 seconds later, the server returns `404 Not Found`. Despite the different HTTP status code, the **server's state remains identical** (the room is still gone). Therefore, the operation is idempotent.

---

### Part 3.1: @Consumes and Media Type Mismatches

**Question:** What happens if a client sends data in a different format (e.g., text/plain) when @Consumes(MediaType.APPLICATION_JSON) is specified?

**Answer:**  
If a client attempts to send XML or plain text to a method annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the JAX-RS runtime intercepts the request **before it reaches the Java method**. Because the server cannot find a suitable `MessageBodyReader` to deserialize the unsupported format, JAX-RS automatically aborts the request and returns an HTTP **`415 Unsupported Media Type`** error, protecting the application from parsing exceptions.

---

### Part 3.2: Query Parameters vs. Path Parameters

**Question:** Contrast @QueryParam with using path parameters for filtering (e.g., `/sensors/type/CO2`).

**Answer:**  
Path parameters (`/sensors/{id}`) uniquely identify a specific resource within a hierarchy. Query parameters (`/sensors?type=CO2`) filter, sort, or paginate an existing collection. Query parameters are superior because the **core identity of the resource collection** (`/sensors`) does not change. Using paths (`/sensors/type/CO2`) implies that "type" is a definitive structural sub-resource, which becomes **inflexible** when chaining multiple filters (e.g., `?type=CO2&status=ACTIVE`).

---

### Part 4.1: Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern.

**Answer:**  
The Sub-Resource Locator pattern (delegating `/{sensorId}/readings` to a `SensorReadingResource` class) drastically improves architectural **maintainability**. It prevents the main `SensorResource` from bloating into a monolithic "God class" containing thousands of lines of code. By delegating nested routing to smaller, context-aware classes, we adhere to the **Single Responsibility Principle**, making large APIs significantly easier to test, debug, and extend.

---

### Part 5.2: 422 Unprocessable Entity vs. 404 Not Found

**Question:** Why is HTTP 422 often considered more semantically accurate than 404 for missing payload references?

**Answer:**  
Returning a `404 Not Found` implies that the target URI itself (e.g., `POST /sensors`) does not exist. However, if a client tries to create a sensor using a `roomId` that doesn't exist, the URI is perfectly valid and the JSON syntax is correct. The failure is a **semantic validation error inside the payload** (a broken foreign key). HTTP `422 Unprocessable Entity` is semantically accurate: "I understand your request and the JSON is valid, but the business instructions contained within are **impossible to process**."

---

### Part 5.4: Cybersecurity Risks of Stack Traces

**Question:** From a cybersecurity standpoint, explain the risks of exposing Java stack traces.

**Answer:**  
Exposing raw Java stack traces is a critical **Information Disclosure** vulnerability. A stack trace reveals the exact framework being used, third-party library versions, internal server directory paths, and the logical flow of the application. An attacker can use this reconnaissance data to identify **outdated libraries with known CVEs** (Common Vulnerabilities and Exposures) or map out the backend architecture to craft highly targeted injection attacks. My `GlobalExceptionMapper` catches all `Throwable` exceptions and returns sanitized JSON responses, preventing this leak.

---

### Part 5.5: JAX-RS Filters for Cross-Cutting Concerns

**Question:** Why is it advantageous to use JAX-RS filters for logging rather than manually inserting Logger statements?

**Answer:**  
Using JAX-RS `ContainerRequestFilter` and `ContainerResponseFilter` implements **Aspect-Oriented Programming (AOP)**. Logging is a "cross-cutting concern" that applies to every endpoint. Manually inserting `Logger.info()` into every resource method violates the **DRY (Don't Repeat Yourself)** principle and clutters business logic. Filters intercept traffic **globally at the framework edge**, ensuring consistent observability without coupling logging mechanisms to the core application logic.

---

## 6. Project Structure

```
SmartCampusAPI/
├── pom.xml                          # Maven configuration
├── README.md                       # This file
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

## 7. Error Responses

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

**Good luck with your submission!** 🚀