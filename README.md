# Smart Campus Sensor & Room Management API

## 1. Overview
The Smart Campus API is a robust, scalable RESTful web service built strictly using JAX-RS (Jakarta RESTful Web Services). It manages campus infrastructure, including Rooms, Sensors (Temperature, CO2, Occupancy), and historical Sensor Readings. 

To ensure thread-safety without relying on an external database, the system utilizes a Singleton `DataStore` backed by `ConcurrentHashMap` and `CopyOnWriteArrayList`. This prevents race conditions and data corruption during concurrent API requests.

## 2. Build & Run Instructions
This project is built using Maven and packaged as a `.war` file.

**Prerequisites:**
* Java 11 or higher
* Maven 3.6+
* A Jakarta EE / Java EE application server (e.g., Apache Tomcat 9+, Payara, or GlassFish)

**Steps:**
1. Clone the repository to your local machine.
2. Open a terminal in the root directory (where `pom.xml` is located).
3. Compile and build the project:
   ```bash
   mvn clean install
   ```
4. Deploy the generated `target/SmartCampusAPI-1.0-SNAPSHOT.war` file to your application server's `webapps` directory (or run it directly from your IDE like Apache NetBeans or IntelliJ).
5. The API will be accessible at: `http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1`

## 3. Sample cURL Commands

**1. Discovery Endpoint (HATEOAS)**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1 -H "Accept: application/json"
```

**2. Create a Room**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"name": "Lecture Theatre 1", "capacity": 150}'
```

**3. Get All Rooms**
```bash
curl -X GET http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/rooms -H "Accept: application/json"
```

**4. Register a Sensor (Replace ROOM_ID with an actual ID from Step 2)**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/sensors \
-H "Content-Type: application/json" \
-d '{"type": "CO2", "roomId": "ROOM_ID"}'
```

**5. Record a Sensor Reading (Replace SENSOR_ID with an actual ID from Step 4)**
```bash
curl -X POST http://localhost:8080/SmartCampusAPI-1.0-SNAPSHOT/api/v1/sensors/SENSOR_ID/readings \
-H "Content-Type: application/json" \
-d '{"value": 415.5}'
```

---

## 4. Technical Report (Coursework Questions)

### Part 1.1: JAX-RS Lifecycle & Thread Safety
By default, the JAX-RS runtime uses a **Request-Scoped** lifecycle. This means a brand new instance of a Resource class (e.g., `RoomResource`) is instantiated for every single incoming HTTP request, and then garbage collected. If multiple clients hit the API simultaneously, multiple threads process these requests concurrently. To prevent race conditions and data loss in the absence of a relational database, the in-memory data structures must be decoupled from the Resource lifecycle. I implemented a synchronized Singleton `DataStore` using `ConcurrentHashMap` and `CopyOnWriteArrayList`. These concurrent collections use fine-grained locking (like bucket-level locks), ensuring thread-safe reads and writes across simultaneous API calls.

### Part 1.2: HATEOAS & Hypermedia
Hypermedia as the Engine of Application State (HATEOAS) allows an API to return navigational links (`_links`) alongside data. This is a hallmark of advanced RESTful design because it makes the API self-discoverable. Client developers do not need to hardcode URL paths or rely heavily on static, potentially outdated documentation. Instead, the client dynamically follows the URIs provided by the server, drastically reducing tight coupling between the frontend and backend architectures.

### Part 2.1: Returning IDs vs. Full Objects
Returning only a list of IDs for a collection (e.g., `["LIB-301", "LIB-302"]`) minimizes network bandwidth and payload overhead. However, it forces the client to suffer the "N+1 query problem," requiring them to make subsequent HTTP GET requests for every single ID to render a UI. Conversely, returning full objects increases payload size and server-side JSON serialization time, but allows the client to process and render the data immediately in a single network round-trip. The choice depends on the specific UX requirements and mobile network constraints.

### Part 2.2: Idempotency of the DELETE Operation
The `DELETE` operation in this implementation is strictly idempotent. Idempotency means that making multiple identical requests has the exact same effect on the *server's state* as making a single request. If a client successfully deletes `Room A`, it returns `204 No Content`. The server state is modified (the room is gone). If the client sends the exact same `DELETE` request 5 seconds later, the server returns `404 Not Found`. Despite the different HTTP status code, the *server's state* remains identical (the room is still gone). Therefore, the operation is idempotent.

### Part 3.1: @Consumes and Media Type Mismatches
If a client attempts to send XML or plain text to a method annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the JAX-RS runtime intercepts the request before it even reaches the Java method. Because the server cannot find a suitable MessageBodyReader to deserialize the unsupported format, JAX-RS automatically aborts the request and returns an HTTP `415 Unsupported Media Type` error to the client, protecting the application from parsing exceptions.

### Part 3.2: Query Parameters vs. Path Parameters
Path parameters (`/sensors/{id}`) are used to uniquely identify a specific resource within a hierarchy. Query parameters (`/sensors?type=CO2`) are used to filter, sort, or paginate an existing collection. Using query parameters for filtering is superior because the core identity of the resource collection (`/sensors`) does not change. If we used paths (`/sensors/type/CO2`), it implies that "type" is a definitive structural sub-resource, which becomes inflexible when attempting to chain multiple filters (e.g., `?type=CO2&status=ACTIVE`).

### Part 4.1: Sub-Resource Locator Pattern
The Sub-Resource Locator pattern (e.g., delegating `/{sensorId}/readings` to a `SensorReadingResource` class) drastically improves architectural maintainability. It prevents the main `SensorResource` from bloating into a monolithic "God class" containing thousands of lines of code. By delegating nested routing to smaller, context-aware classes, we adhere to the Single Responsibility Principle, making large APIs significantly easier to test, debug, and extend.

### Part 5.2: 422 Unprocessable Entity vs. 404 Not Found
Returning a `404 Not Found` implies that the target URI itself (e.g., `POST /sensors`) does not exist on the server. However, if a client tries to create a sensor using a `roomId` that doesn't exist, the URI is perfectly valid, and the JSON syntax is correct. The failure is a semantic validation error *inside* the payload (a broken foreign key). HTTP `422 Unprocessable Entity` is semantically accurate here, telling the client: "I understand your request and the JSON is valid, but the business instructions contained within are impossible to process."

### Part 5.4: Cybersecurity Risks of Stack Traces
Exposing raw Java stack traces to API consumers is a critical security vulnerability known as Information Disclosure. A stack trace reveals the exact framework being used, third-party library versions, internal server directory paths, and the logical flow of the application. An attacker can use this reconnaissance data to identify outdated libraries with known CVEs (Common Vulnerabilities and Exposures) or map out the backend architecture to craft highly targeted injection attacks.

### Part 5.5: JAX-RS Filters for Cross-Cutting Concerns
Using JAX-RS `ContainerRequestFilter` and `ContainerResponseFilter` implements Aspect-Oriented Programming (AOP). Logging is a "cross-cutting concern"—it applies to every endpoint. Manually inserting `Logger.info()` into every resource method violates the DRY (Don't Repeat Yourself) principle and clutters business logic. Filters intercept traffic globally at the framework edge, ensuring consistent observability without coupling logging mechanisms to the core application logic.