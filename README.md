# Workshop on Designing and Structuring Distributed Internet Applications

This project is a minimal HTTP server implemented with raw **Java sockets**. It is meant as an academic exercise to understand how HTTP works under the hood, without using frameworks like Spring Boot or Spark.

## Getting Started

Before you run this project, make sure you have the following installed:
### Prerequisites

- **Java JDK 8+**  
  Verify installation:
  ```bash
  java -version
  ```
  Example output:
    ```  
    openjdk version "17.0.9" 2023-10-17
    ```
- **Maven 3.6+ (to build and run the project)**
    Verify installation:
    ```
    mvn -v
    ```
    Example output:
    ```
    Apache Maven 3.9.6
    ```
- An IDE (optional): IntelliJ IDEA is recommended for easier development and debugging.
    
### Installing

A step-by-step series of examples that tell you how to get a development environment running.

**Step 1 - Clone or download the repository**
```
git clone https://github.com/Jaco0bo/ejercicio1AREP.git
cd java-socket-http-server
```

**Step 2 - Build the project with Maven**
```
mvn clean package
```

This will download dependencies and generate compiled classes under the target directory.

**Step 3 - Run the server**

You can run the server from the terminal:
```
mvn exec:java -Dexec.mainClass="org.escuelaing.edu.co.HttpServer"
```

Expected output:
```bash
Listening on http://localhost:36000/
```

## Running the tests

This project includes a set of automated tests to ensure that the socket server and client work as expected. The tests cover both end-to-end functionality and coding style.

### End-to-end tests

End-to-end tests simulate a real interaction between the client and the server. These tests ensure that when a client sends a message to the server, the server correctly echoes the message back.

Example:

From the project, run:
```bash
HttpServerTest.java class
```
If successful, you should see output confirming that the test passed and confirmating the correct operation of the class.

## Deployment

Because this is an academic exercise, you can´t deploy the Http server to a live environment to allow multiple clients to connect :(.

## Built With

This project was built with the following tools and libraries:

- **Java** — Core programming language used to implement the socket HTTP server.  
- **Maven** — Build tool and dependency management (`pom.xml` included).  
- **JUnit 5** — Unit testing framework used for automated tests.  
- **IntelliJ IDEA** — Recommended IDE for development and debugging (optional).

> Note: This project does **not** use Spark, Spring Boot, or other web frameworks — it is intentionally minimal and socket-based for learning purposes.

---

## Contributing

If you would like to contribute, please:

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/my-change`
3. Run the tests locally: `mvn test`
4. Commit your changes: `git commit -m "Add my feature"`
5. Push to the branch: `git push origin feature/my-change`
6. Open a Pull Request describing the change and why it is useful.

## Authors

- **Jacobo Sepulveda** — Initial work (author).  
  - GitHub: `https://github.com/Jaco0bo` *

## Acknowledgments

- Hat tip to classic low-level networking examples and tutorials that explain sockets and HTTP basics.  
- Inspiration: articles and books on TCP/IP and HTTP internals.  
- Thank you to the teacher for the questions resolved in class and a lot of internet web pages and IA help.

## Demo


