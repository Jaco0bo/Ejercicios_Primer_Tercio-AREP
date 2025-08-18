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
git clone https://github.com/your-username/java-socket-http-server.git
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
mvn exec:java -Dexec.mainClass="org.example.HttpServer"
```

Expected output:
```bash
Listening on http://localhost:36000/
```
