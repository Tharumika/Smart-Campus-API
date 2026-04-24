# Technical Report: Smart Campus Sensor & Room Management API

---

## Part 1.1: JAX-RS Lifecycle & Thread Safety

**Question:** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

**Answer:**
By default, the JAX-RS runtime uses a **Request-Scoped** lifecycle. A brand new instance of a Resource class (e.g., `RoomResource`) is instantiated for **every single incoming HTTP request**, and then garbage collected after the response is sent. This means that if 100 sensors send data simultaneously, 100 separate Java threads create 100 different `RoomResource` instances.

To prevent race conditions and data loss, the in-memory data structures must be **decoupled from the Resource lifecycle**. I implemented a synchronized Singleton `DataStore` using `ConcurrentHashMap` and `CopyOnWriteArrayList`. These concurrent collections use fine-grained locking (bucket-level locks), ensuring thread-safe reads and writes across simultaneous API calls without blocking the entire data store.

---

## Part 1.2: HATEOAS & Hypermedia

**Question:** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

**Answer:**
Hypermedia as the Engine of Application State (HATEOAS) allows an API to return navigational links (`_links`) alongside data. This is a hallmark of advanced RESTful design because it makes the API **self-discoverable**. Client developers do not need to hardcode URL paths or rely on static, potentially outdated documentation. Instead, the client dynamically follows the URIs provided by the server, drastically **reducing tight coupling** between the frontend and backend architectures. If the server's routing changes, clients automatically adapt without code updates.

---

## Part 2.1: Returning IDs vs. Full Objects

**Question:** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

**Answer:**
Returning only a list of IDs (e.g., `["LIB-301", "LIB-302"]`) minimizes network bandwidth and payload overhead. However, it forces the client to suffer the **"N+1 query problem"**, requiring subsequent HTTP GET requests for every single ID to render a UI. Conversely, returning full objects increases payload size and serialization time, but allows the client to process and render data immediately in a **single network round-trip**. The choice depends on specific UX requirements and mobile network constraints.

---

## Part 2.2: Idempotency of the DELETE Operation

**Question:** Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.

**Answer:**
Yes, the `DELETE` operation in this implementation is **strictly idempotent**. Idempotency means that making multiple identical requests has the exact same effect on the server's state as making a single request. If a client successfully deletes `Room A`, it returns `204 No Content`. The server state is modified (the room is gone). If the client sends the exact same `DELETE` request 5 seconds later, the server returns `404 Not Found`. Despite the different HTTP status code, the **server's state remains identical** (the room is still gone). Therefore, the operation is idempotent.

---

## Part 3.1: @Consumes and Media Type Mismatches

**Question:** We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?

**Answer:**
If a client attempts to send XML or plain text to a method annotated with `@Consumes(MediaType.APPLICATION_JSON)`, the JAX-RS runtime intercepts the request **before it reaches the Java method**. Because the server cannot find a suitable `MessageBodyReader` to deserialize the unsupported format, JAX-RS automatically aborts the request and returns an HTTP **`415 Unsupported Media Type`** error, protecting the application from parsing exceptions.

---

## Part 3.2: Query Parameters vs. Path Parameters

**Question:** You implemented this filtering using @QueryParam. Contrast this with an alternate design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the queryparameterapproach generally considered superior for filtering and searching collections?

**Answer:**
Path parameters (`/sensors/{id}`) uniquely identify a specific resource within a hierarchy. Query parameters (`/sensors?type=CO2`) filter, sort, or paginate an existing collection. Query parameters are superior because the **core identity of the resource collection** (`/sensors`) does not change. Using paths (`/sensors/type/CO2`) implies that "type" is a definitive structural sub-resource, which becomes **inflexible** when chaining multiple filters (e.g., `?type=CO2&status=ACTIVE`).

---

## Part 4.1: Sub-Resource Locator Pattern

**Question:** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive controller class?

**Answer:**
The Sub-Resource Locator pattern (delegating `/{sensorId}/readings` to a `SensorReadingResource` class) drastically improves architectural **maintainability**. It prevents the main `SensorResource` from bloating into a monolithic "God class" containing thousands of lines of code. By delegating nested routing to smaller, context-aware classes, we adhere to the **Single Responsibility Principle**, making large APIs significantly easier to test, debug, and extend.

---

## Part 5.2: 422 Unprocessable Entity vs. 404 Not Found

**Question:** Why is HTTP422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

**Answer:**
Returning a `404 Not Found` implies that the target URI itself (e.g., `POST /sensors`) does not exist. However, if a client tries to create a sensor using a `roomId` that doesn't exist, the URI is perfectly valid and the JSON syntax is correct. The failure is a **semantic validation error inside the payload** (a broken foreign key). HTTP `422 Unprocessable Entity` is semantically accurate: "I understand your request and the JSON is valid, but the business instructions contained within are **impossible to process**."

---

## Part 5.4: Cybersecurity Risks of Stack Traces

**Question:** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

**Answer:**
Exposing raw Java stack traces is a critical **Information Disclosure** vulnerability. A stack trace reveals the exact framework being used, third-party library versions, internal server directory paths, and the logical flow of the application. An attacker can use this reconnaissance data to identify **outdated libraries with known CVEs** (Common Vulnerabilities and Exposures) or map out the backend architecture to craft highly targeted injection attacks. My `GlobalExceptionMapper` catches all `Throwable` exceptions and returns sanitized JSON responses, preventing this leak.

---

## Part 5.5: JAX-RS Filters for Cross-Cutting Concerns

**Question:** Why is it advantageous to use JAX-RS filters for logging, rather than manually inserting Logger statements inside every single resource method?

**Answer:**
Using JAX-RS `ContainerRequestFilter` and `ContainerResponseFilter` implements **Aspect-Oriented Programming (AOP)**. Logging is a "cross-cutting concern" that applies to every endpoint. Manually inserting `Logger.info()` into every resource method violates the **DRY (Don't Repeat Yourself)** principle and clutters business logic. Filters intercept traffic **globally at the framework edge**, ensuring consistent observability without coupling logging mechanisms to the core application logic.