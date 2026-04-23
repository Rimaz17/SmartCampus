# Smart Campus — Sensor & Room Management API

## Overview

This is a RESTful API built with JAX-RS (Jersey 2.34) and deployed on Apache Tomcat 9. It provides a backend system for managing campus rooms and the sensors deployed within them, including a full historical reading log per sensor.

The API follows REST architectural principles: resource-based URLs, appropriate HTTP status codes, JSON request/response bodies, and a logical resource hierarchy that mirrors the physical structure of the campus.

### Resource Hierarchy
/api/v1/ → Discovery endpoint 
/api/v1/rooms → Room collection
/api/v1/rooms/{roomId} → Single room
/api/v1/sensors → Sensor collection (supports ?type= filter) 
/api/v1/sensors/{sensorId} → Single sensor 
/api/v1/sensors/{sensorId}/readings → Reading history (sub-resource)

### Data Models

**Room** - represents a physical campus room. Fields: `id`, `name`, `capacity`, `sensorIds`.

**Sensor** - represents a hardware sensor deployed in a room. Fields: `id`, `type`, `status` (ACTIVE / MAINTENANCE / OFFLINE), `currentValue`, `roomId`.

**SensorReading** -  represents a single historical measurement. Fields: `id` (UUID), `timestamp` (epoch ms), `value`.

### Design Decisions

- All data is stored in-memory using `ConcurrentHashMap` and `ArrayList` - no database is used.
- A singleton `DataStore` class holds all application state, since JAX-RS resource classes are request-scoped (a new instance per request) and cannot hold state themselves.
- Exception mappers handle all error scenarios and guarantee no raw stack traces are ever returned to the client.
- A logging filter records every request method/URI and response status code automatically.

---

## How to Build and Run

### Prerequisites

- Java 8 or higher
- Apache Maven 3.6+
- Apache Tomcat 9.x ([download here](https://tomcat.apache.org/download-90.cgi))
- NetBeans IDE (or any IDE with Maven support)

### Step 1 — Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/SmartCampus.git
cd SmartCampus
```

### Step 2 — Build the WAR file

```bash
mvn clean package
```

This produces `target/SmartCampus-1.0-SNAPSHOT.war`.

### Step 3 — Deploy to Tomcat

Copy the WAR file into your Tomcat `webapps` folder:

```bash
cp target/SmartCampus-1.0-SNAPSHOT.war /path/to/apache-tomcat-9.x/webapps/
```

### Step 4 — Start Tomcat

```bash
/path/to/apache-tomcat-9.x/bin/startup.sh    # Mac/Linux
/path/to/apache-tomcat-9.x/bin/startup.bat   # Windows	
```

### Step 5 — Verify it is running

Open your browser and visit:
http://localhost:8080/SmartCampus/api/v1/

You should see a JSON discovery response with API metadata and resource links.

> **In NetBeans:** Right-click the project → Run. NetBeans will build and deploy to Tomcat automatically. The base URL will be `http://localhost:8080/SmartCampus/api/v1/`.

---

## Sample curl Commands

### 1. Discovery — get API metadata and resource links

```bash
curl -X GET http://localhost:8080/SmartCampus/api/v1/
```

### 2. Create a room

```bash
curl -X POST http://localhost:8080/SmartCampus/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id": "LIB-301", "name": "Library Quiet Study", "capacity": 50}'
```

### 3. Create a sensor linked to that room

```bash
curl -X POST http://localhost:8080/SmartCampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-001", "type": "Temperature", "status": "ACTIVE", "currentValue": 0.0, "roomId": "LIB-301"}'
```

### 4. Post a sensor reading (updates the sensor's currentValue automatically)

```bash
curl -X POST http://localhost:8080/SmartCampus/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 22.5}'
```

### 5. Filter sensors by type

```bash
curl -X GET "http://localhost:8080/SmartCampus/api/v1/sensors?type=Temperature"
```

### 6. Attempt to delete a room that still has sensors (returns 409 Conflict)

```bash
curl -X DELETE http://localhost:8080/SmartCampus/api/v1/rooms/LIB-301
```

### 7. Attempt to register a sensor with a non-existent roomId (returns 422 Unprocessable Entity)

```bash
curl -X POST http://localhost:8080/SmartCampus/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id": "TEMP-002", "type": "Temperature", "status": "ACTIVE", "currentValue": 0.0, "roomId": "FAKE-999"}'
```

---

## Report

The written answers to all coursework questions are provided below, organised by part.

### Part 1.1 - In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.
By default, JAX-RS follows a request-scoped lifecycle: a brand new instance of each resource class (e.g., RoomResource, SensorResource) is instantiated for every single incoming HTTP request, and that instance is discarded immediately after the response is sent. The runtime does not treat resource classes as singletons by default.
This has a critical implication for state management. Because each request gets a fresh resource object, any instance-level fields (e.g., a HashMap declared inside a resource class) would be initialised empty on every request. This means data stored by one request would be completely invisible to the next. Storing application data directly in resource fields is therefore impossible under the default lifecycle.
To solve this, all data was centralised inside a DataStore singleton, a single shared instance that lives for the entire lifetime of the application. Every request-scoped resource instance accesses the same DataStore.getInstance() reference, so data persists correctly across all requests.
However, because multiple HTTP requests can arrive simultaneously in a multi-threaded servlet container such as Tomcat, storing data in a plain HashMap would create race conditions. Two threads could modify the map at the same time, leading to corrupted data or silent data loss. For example, a put from one thread could overwrite an incomplete put from another thread. To prevent this, ConcurrentHashMap was used for all top-level maps. Unlike HashMap, ConcurrentHashMap divides the map into segments and locks only the affected segment during a write, allowing concurrent reads and writes without full synchronisation overhead. For the sensor readings lists (inner ArrayLists), a synchronized block on the specific list is additionally used before appending, since ArrayList itself is not thread-safe.

### Part 1.2 - Why is the provision of ”Hypermedia” (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?
HATEOAS (Hypermedia as the Engine of Application State) is the principle that API responses should include links to related resources and available actions, allowing clients to navigate the API dynamically rather than relying on hardcoded URLs or external documentation.
It is considered a hallmark of advanced REST design because it fully decouples the client from the server's internal URI structure. A client that follows links embedded in responses does not need to know in advance where resources live — it discovers them at runtime from the API itself. This mirrors how a web browser works: a user does not memorise all URLs on a website; they click links presented to them.
Compared to static documentation, HATEOAS offers several concrete benefits. First, if the server changes a resource path (e.g., /api/v1/rooms becomes /api/v2/rooms), clients that follow hypermedia links adapt automatically without code changes, whereas clients hardcoding URLs from static docs break immediately. Second, responses can advertise only the actions currently available given the resource's state — for example, a room with active sensors could omit the delete link, making the API self-guiding and reducing invalid requests. Third, it reduces the risk of documentation becoming stale, since the API itself is the source of truth for navigation. This ultimately leads to more resilient, maintainable client applications and lower integration costs.

### Part 2.1 When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.
Returning only IDs is bandwidth-efficient for the initial list response because the payload is small regardless of how many rooms exist. However, it forces the client to make N additional HTTP requests (one per room ID) to retrieve the details it actually needs. This is a well-known anti-pattern called the N+1 request problem. Under high load or poor network conditions, these extra round-trips add significant latency and increase server load multiplicatively.
Returning full room objects in a single response eliminates those follow-up calls. The client receives everything it needs in one network round-trip, which is far more efficient in the overwhelming majority of real-world scenarios. The bandwidth cost of a larger single payload is almost always less than the combined overhead of many small HTTP requests, each carrying its own headers, TCP handshake cost, and server processing.
The trade-off tilts toward IDs only when the dataset is extremely large (thousands of rooms) and clients typically need just a handful of records. In that case, pagination combined with full objects per page is the standard industry solution, not returning IDs and forcing N+1 lookups. For a campus management system of realistic scale, returning full objects is the correct design.

### Part 2.2 – Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.
The DELETE operation in this implementation is idempotent with respect to server state, but not with respect to the response code. This distinction is worth explaining carefully.
RFC 9110 (HTTP semantics) defines idempotency as follows: multiple identical requests have the same effect on server state as a single request. By this definition, DELETE is idempotent. After the first successful DELETE /api/v1/rooms/LIB-301, the room no longer exists in the DataStore. A second, third, or hundredth identical request cannot delete it again. The server state remains exactly the same (room absent) after every subsequent call.
However, the HTTP response differs. The first DELETE returns 200 OK (room found and deleted). Every subsequent DELETE returns 404 Not Found (room is already gone). This means the response code changes, even though the underlying state does not.
This is the correct and standard behaviour. The 404 on repeat calls does not violate idempotency because it accurately reflects the current state of the resource. Some APIs return 204 No Content for all DELETE calls, including when the resource is already absent, which makes the response code also idempotent. However, returning 404 is equally valid and arguably more transparent, as it tells the client the resource was never there or was already removed. In practice, idempotency is what matters for safe retry logic. A client that retries a DELETE after a network timeout can do so safely, knowing it will never accidentally delete something twice.

### Part 3.1 - Weexplicitly use the @Consumes (MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?
The @Consumes(MediaType.APPLICATION_JSON) annotation tells the JAX-RS runtime that the annotated method will only accept request bodies with a Content-Type: application/json header. This is enforced automatically by the framework before the method body is ever executed.
If a client sends a request with Content-Type: text/plain or Content-Type: application/xml, JAX-RS performs content negotiation and finds no matching resource method for that media type. It immediately rejects the request and returns HTTP 415 Unsupported Media Type without invoking the resource method at all. The client receives this error response and must correct its Content-Type header before retrying.
This is beneficial for several reasons. The resource method is protected from receiving data it cannot parse. There is no risk of passing an XML string to a JSON deserialiser and getting a confusing runtime error. It also provides clear, standards-compliant feedback to the client, telling the client exactly what the problem is (the format, not the content). It enforces a contract at the transport layer: the API is documented as JSON-only, and the server enforces this mechanically. Developers integrating the API receive an immediate, unambiguous signal rather than a cryptic 500 error caused by a failed parse deep inside a resource method.

### Part 3.2 – You implemented thisfiltering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/vl/sensors/type/CO2). Why is the queryparameterapproachgenerallyconsideredsuperiorforfilteringandsearching collections?
A path parameter (for example, /api/v1/sensors/type/CO2) implies that CO2 is a distinct, addressable resource in the API's hierarchy. This would be something a client can bookmark, cache with a unique key, and navigate to directly. This is semantically correct for identifiers (such as /sensors/TEMP-001 where TEMP-001 is a specific sensor), but wrong for filtering, because a filter is not a resource. It is a query against a collection.
Query parameters (?type=CO2) are semantically correct for filtering because they represent optional, transient constraints applied to a collection request. Several concrete advantages follow from this.
The base collection URL (/api/v1/sensors) remains clean and stable regardless of how many filter dimensions exist. Adding a second filter criterion is trivial with query parameters (?type=CO2&status=ACTIVE) but would require a deeply nested and increasingly ugly path structure (/sensors/type/CO2/status/ACTIVE) that is hard to read and even harder to extend. Query parameters are also optional by nature. Omitting ?type simply returns all sensors, whereas a path-based design would require a separate route for the unfiltered case. Furthermore, query parameters are the established convention for search and filtering across virtually every major REST API (GitHub, Twitter, Stripe), meaning developers have an existing mental model for them. Finally, REST resource paths should be noun-based hierarchies representing things; query strings represent how a client wants to view or filter those things. Using path segments for filtering blurs this conceptual boundary and undermines the clarity of the API's resource model.

### Part 4.1 - Discuss the architectural benefits of the Sub-Resource Locator pattern. How doesdelegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., sensors/{id}/readings/{rid}) in one massive con troller class?
The Sub-Resource Locator pattern allows a resource method to return an object of another class rather than a response directly. JAX-RS then delegates the remaining URL resolution and method dispatch to that returned object. In this implementation, SensorResource contains a locator method annotated with @Path("/{sensorId}/readings") that returns a SensorReadingResource instance. JAX-RS then handles GET /readings and POST /readings by inspecting that class.
The architectural advantages of the system become more important because APIs continue to expand. The Single Responsibility Principle provides its first advantage through the SensorResource class which handles sensor registration and retrieval operations while SensorReadingResource class handles its exclusive mission of reading history management. The two classes operate independently because they do not require knowledge about each other's internal processes. If the readings feature needs to change (for example, adding pagination or aggregation), only SensorReadingResource is touched. 
In a monolithic controller approach, where one class handles /sensors, /sensors/{id}, /sensors/{id}/readings, and /sensors/{id}/readings/{rid}, the class grows unbounded as the API expands. The testing process requires complete controller setup because method names create conflicts and instance variable definitions lead to variable scope problems. The process of code review becomes more difficult while debugging work becomes harder and new developer onboarding shows increasing challenges.
With sub-resource locators, each class is independently testable, can be developed by separate team members simultaneously, and can be swapped or replaced without touching the parent resource. The locator also enables the parent to inject context (such as the sensorId) into the sub-resource at construction time, making the relationship explicit and clean rather than relying on shared mutable state. This composability is what makes JAX-RS sub-resources a genuinely scalable architectural pattern for real-world APIs.

### Part 5.2 – Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?
HTTP 404 Not Found means the requested URL does not correspond to any resource on the server. If a client sends POST /api/v1/sensors and that endpoint exists, returning 404 is factually incorrect because the endpoint was found. It misleads the client into thinking the URL is wrong, when the problem is actually inside the request body.
HTTP 422 Unprocessable Entity means the server understood the request (correct URL, correct Content-Type, valid JSON syntax) but cannot process it because the semantic content is invalid. In this case, the JSON is syntactically correct. It has all required fields, but the value of roomId references an entity that does not exist in the system. The document is well-formed but semantically broken.
The 422 status code explains the failure because the server failed to execute the request after receiving it because a specific component in the request payload failed to exist. The developer needs to examine the request body while ignoring the URL according to this information. The system provides improved error handling capabilities to client applications. A 404 error will display either a redirect to another page or a "resource not found" message, but a 422 error will show form validation results and require users to correct their payload inputs. The semantically accurate status code distinguishes a professional API design from an ordinary API design because it supports the HTTP status code principle which states that codes should describe results instead of indicating whether operations succeeded or failed.

### Part 5.4 - From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?
Exposing raw Java stack traces to external consumers is a serious information disclosure vulnerability, listed under OWASP A05: Security Misconfiguration. A stack trace that would be helpful to a developer in a local environment becomes a reconnaissance tool for an attacker in production.

Specifically, an attacker can extract the following from a stack trace. Internal class and package names reveal the application's architecture. Package names like com.mycompany.smartcampus.store.DataStore tell an attacker how the codebase is structured and where business logic lives. Third-party library names and exact versions (for example, org.glassfish.jersey.server.ServerRuntime version 2.34) allow the attacker to look up known CVEs (Common Vulnerabilities and Exposures) for that exact version and craft targeted exploits. Server file system paths (such as /opt/tomcat/webapps/SmartCampus/WEB-INF/classes/...) disclose the server's directory layout, which is useful for path traversal attacks. Business logic flow can be inferred from the sequence of method calls, revealing how data is validated, processed, and stored. This information helps an attacker bypass checks. Database queries or connection strings occasionally appear in traces from ORM or JDBC layers, potentially exposing credentials or schema details.

The system at GlobalExceptionMapper handles error resolution by implementing a solution that involves server-side logging of complete stack traces which only authorized developers can access while it delivers a standard 500 Internal Server Error message to clients. The system applies the principle of least privilege which grants external users access to only the necessary information they need to know about the system malfunction.

### Part 5.5 - Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single re source method?
Cross-cutting concerns are behaviours that apply uniformly across the entire application rather than belonging to any single business function. Logging, authentication, CORS headers, and rate limiting are all cross-cutting. They have nothing to do with whether a request is creating a room or fetching a sensor, but they must happen for every request regardless.
Placing Logger.info() calls manually inside every resource method violates the DRY (Don't Repeat Yourself) principle in the most direct way possible. With four resource classes and a dozen methods, that is already a dozen places where logging code must be written, maintained, and kept consistent. In a real API with hundreds of endpoints, manual logging becomes practically unmanageable.
JAX-RS filters solve this cleanly. The LoggingFilter class registers once with the runtime via @Provider, and JAX-RS guarantees it runs for every request and response automatically, including any future resource classes added to the project. A developer adding a new endpoint gets logging for free without writing a single logging line.
Beyond DRY, filters enforce consistency: every log line follows the same format, logged at the same level, from the same logger. Manual insertion is prone to differences in format, accidental omission on new methods, or logging at the wrong level. Filters also enable easy replacement or extension. Switching from java.util.logging to SLF4J, or adding a request ID to every log line, requires changing one file rather than hunting through every resource class. This is the essence of the separation of concerns design principle: resource methods stay focused entirely on business logic, and the infrastructure layer handles infrastructure concerns.



