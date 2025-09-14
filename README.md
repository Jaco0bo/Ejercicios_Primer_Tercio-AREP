# Workshop on modularization with virtualization and introduction to Docker

## Andrés Jacobo Sepúlveda Sánchez

**For this assignment, you must build a web application and deploy it to AWS using EC2 and Docker. For deployment, you must use their framework (DO NOT USE SPRING). You must enhance your framework to make it concurrent and able to shut down gracefully.**

## Project Summary

This project implements an HTTP server in Java with the following features:

- **Annotated controllers** (@RestController, @GetMapping, @RequestParam) discovered via reflection.
- **Dynamic routing** to Java methods using a Router.
- **Static file handling** (searches in ./public, src/main/resources/public, and within the JAR).
- **Safe concurrency** using ThreadPoolExecutor and ServerController for controlled startup and shutdown.
- **Automated testing** with JUnit 5, including concurrency tests that start/stop the server from within the tests.
- **Packaged in a fat JAR** (jar-with-dependencies) and deployed via Docker.

---

## Prerequisites

- Java 21 (JDK)

- Maven 3.8+ (or latest)

- Docker (for container builds)

Optional: curl, ab (ApacheBench) or other load tools for testing concurrency

## Architecture and General Design

```
Client (browser/curl) 
  ↓ HTTP
ServerController (accepts sockets) 
  ↓ delegate
ThreadPoolExecutor (worker pool) 
  ↓ each worker
HttpServer.handle(Socket, Router) ← RequestParser → Request 
                    Router.handle(req, res) → handler 
                                  ↑ 
                            Controller methods (invoked by reflection)
Static files handler (filesystem → project resources → classpath)
```

### Core Components

- **HttpServer**
- Entry point (`main`), loads routes, and mounts the `Router`.
- Defines public folders (`PUBLIC`), registers annotated handlers and fixed routes (`/hello`, `/api/echo`, `/api/time`).
- Starts the `ServerController` with the `ServerSocket` and thread pool.

- **ServerController**
- Encapsulates `ServerSocket`, `ThreadPoolExecutor`, and the `accept()` loop.
- Provides `start(port)` and `stop()` methods for graceful shutdown.

- **Router**
- Maps routes to handlers (`GET`, `POST`).
- `get(path, handler)`, `post(path, handler)`, and `staticFiles(baseDir)` methods.

- **RequestParser**
- Reads raw HTTP from the socket, parses request lines, headers, and body.
- Handles exceptions and returns a `Request` object.

- **Request / Response**
- `Request` stores methods, routes, parameters, headers, and bodies.
- `Response` sends status, headers, and bodies to the client.

- **Annotated Controllers**
- `@RestController` (class), `@GetMapping` (method), `@RequestParam` (parameter).
- Example: `GreetingController` (uses `AtomicLong`), `HelloController`.

---

## Class Design (High Level)

- **org.escuelaing.edu.co.HttpServer**
- `main(String[] args)` — starts, discovers controllers, mounts `Router`, creates `ServerController`.
- `loadRoutesFromClass(Class<?>)` — validates and registers annotated methods.
- `handle(Socket client, Router router)` — request → response flow.

- **org.escuelaing.edu.co.ServerController**
- `start(int port)`
- `stop()`
- Constructs `ThreadPoolExecutor`, thread acceptor.

- **org.escuelaing.edu.co.Router** 
- `get(String path, Handler handler)` 
- `post(String path, Handler handler)` 
- `handle(Request req, Response res)` — searches for route and executes handler. 
- (Optional) `staticFiles(String baseDir)`.

- **org.escuelaing.edu.co.RequestParser** 
- `static Request parse(Socket client, int timeoutMillis)`

- **org.escuelaing.edu.co.annotations** 
- `@RestController`, `@GetMapping`, `@RequestParam`

---

## How to Build and Run (Local)

**Requirements:** Java 21, Maven 3.8+, Docker.

1. **Build with Maven:**
```bash
mvn clean package
```
This generates the JAR in `target/` (using `maven-assembly-plugin` you will have `*-jar-with-dependencies.jar`).

2. **Run with Maven (development):**
```bash
mvn clean compile exec:java -Dexec.args="9090"
```
> Note: The plugin must have `<classpathScope>runtime</classpathScope>` and `<fork>true</fork>` to include `target/classes`.

3. **Run the JAR:**
```bash
java -jar target/Taller1AREP-1.0-SNAPSHOT-jar-with-dependencies.jar 9090
```
Or, if you don't have the fat JAR:
```bash
java -cp "target/classes;target/dependency/*" org.escuelaing.edu.co.HttpServer 9090
```

## Endpoints (examples)

```GET /``` → ```index.html``` (static)

```GET /greeting?name=Alice``` → annotated controller returns ```Hello, Alice! (id=N)```

```GET /saludo``` → returns HTML greeting

```POST /api/echo``` → returns request body as-is

```GET /api/time``` → returns JSON ```{ "time": "..." }```

```GET /api/otro``` → returns  HTML otro

**Quick test**

```bash
curl "http://localhost:9090/greeting?name=Andres"
curl "http://localhost:9090/"
```

---

## Deployment in Docker

**Dockerfile example:**

```Dockerfile
FROM openjdk:21
WORKDIR /app
COPY src/main/resources/public ./public
COPY target/Taller1AREP-1.0-SNAPSHOT-jar-with-dependencies.jar .
CMD ["java", "-jar", "Taller1AREP-1.0-SNAPSHOT-jar-with-dependencies.jar", "8080"]
```

**Build the image:**
```bash
docker build -t jac0obo8/arep-virtualizacion:2.0 .
```

**Run the container by mapping the port:**
```bash
docker run -d --name taller1 -p 9090:8080 jac0obo8/arep-virtualizacion:2.0
```

---

## Reference images

Pendiente

## Tests (JUnit)

**Running Tests**

```bash
mvn test
```

**Note on Integration/Concurrency Tests**

- Some tests start the server within @BeforeAll (by calling HttpServer.main(new String[]{port})) and stop it within @AfterAll.

- The server must accept args[0] (port) so that tests don't block waiting for console input.

- Tests include:

  - HttpServerTest — unit tests for utilities and routes.

  - ConcurrencyTest — launches N concurrent requests with ExecutorService and verifies responses.

## License

**MIT** 


