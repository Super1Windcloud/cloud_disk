# GEMINI Project Analysis: cloud_disk

## 1. Project Overview

This is a Java-based "Cloud Disk" application. It is built using the **Spring Boot** framework (version 4.0.0) and managed with **Maven**. The project uses **Java 17**.

The application is designed as a web service that likely provides file storage and management capabilities. It connects to a **PostgreSQL** database for data persistence and a **Redis** cache for performance enhancement.

## 2. Core Technologies & Dependencies

- **Framework:** Spring Boot
- **Language:** Java 17
- **Build Tool:** Maven
- **Primary Dependencies:**
    - **`spring-boot-starter-webmvc`**: Indicates this is a web application, likely exposing REST APIs.
    - **`spring-boot-starter-data-jpa`**: Used for database access with PostgreSQL.
    - **`spring-boot-starter-data-redis`**: Used for caching or other Redis-based operations.
    - **`spring-boot-starter-security`**: Implements authentication and authorization.
    - **`spring-boot-starter-thymeleaf`**: Suggests server-side rendered web pages are used.
    - **`postgresql`**: JDBC driver for PostgreSQL.
    - **`lombok`**: To reduce boilerplate Java code.

## 3. Building, Running, and Testing

The project uses the Maven Wrapper (`mvnw`), so a local Maven installation is not required.

### Building
To build the project and create an executable JAR:
```bash
# For Linux/macOS
./mvnw clean install

# For Windows
mvnw.cmd clean install
```

### Running the Application
To run the application locally:
```bash
# For Linux/macOS
./mvnw spring-boot:run

# For Windows
mvnw.cmd spring-boot:run
```
**Pre-requisites:** Before running, ensure that **PostgreSQL** and **Redis** are running and accessible on their default ports (`5432` and `6379` respectively).

### Running Dependent Services
The `compose.yaml` file describes the required services. You can start them using Docker Compose:
```bash
docker compose up
```
*Note: `spring.docker.compose.enabled` is `false` in `application.properties`. This means the Spring Boot application will not start these services for you automatically unless you enable it.*

### Testing
To run the test suite:
```bash
# For Linux/macOS
./mvnw test

# For Windows
mvnw.cmd test
```

## 4. Configuration

The main configuration is in `src/main/resources/application.properties`.

- **Database:** The application connects to a PostgreSQL database named `cloud_disk` at `jdbc:postgresql://localhost:5432/cloud_disk` with user `cloud_disk` and password `cloud_disk`.
- **Hibernate DDL:** `spring.jpa.hibernate.ddl-auto` is set to `update`. This means Hibernate will attempt to automatically update the database schema based on your JPA entities. This is useful for development but should be managed carefully in production.
- **SQL Logging:** `spring.jpa.show-sql=true` will print all executed SQL queries to the console.

## 5. Directory Structure

- **`src/main/java`**: Contains the main application source code. The root package is `org.superwindcloud.cloud_disk`.
- **`src/main/resources`**: Contains configuration files (`application.properties`) and static assets/templates.
- **`src/test/java`**: Contains all test source code.
- **`pom.xml`**: The Maven project definition file, listing all dependencies and build configurations.
- **`compose.yaml`**: Defines the required PostgreSQL and Redis services for Docker.

## 6. API Endpoints (TODO)

A full analysis of controller classes has not been performed. Based on the project's nature, one could expect to find REST endpoints for actions like:

- `POST /files/upload`
- `GET /files/{fileId}`
- `DELETE /files/{fileId}`
- `GET /users/me`

*TODO: Analyze classes annotated with `@RestController` to generate a definitive list of API endpoints and their functions.*

## 7. Development Conventions

- **Code Style:** The use of `lombok` suggests a convention of avoiding manual getters, setters, and constructors. No specific checkstyle or formatter rules are enforced in the build process.
- **Testing:** The presence of various `spring-boot-starter-*-test` dependencies indicates a convention of writing thorough tests for different application layers (web, data, security).
- **Documentation:** The `spring-restdocs-asciidoctor` setup suggests an intent to generate API documentation from tests.
