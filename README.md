# Cloud Disk Quickstart Template

Spring Boot multi-module template for an online cloud storage service. Includes JPA, Redis, security, Thymeleaf UI, OpenAPI, and MinIO client wiring.

## Project Layout
- `domain/` — JPA entities and shared domain models.
- `repository/` — Spring Data repositories.
- `web-api/` — REST controllers and presentation layer.
- `application/` — runnable Spring Boot app (entrypoint).
- `compose.yaml` — Postgres 17 + Redis for local dev. MinIO is included for S3-compatible storage.

## Prerequisites
- Java 17+
- Docker (optional, for Postgres/Redis via Compose)
- Maven Wrapper (`./mvnw`) included; no global Maven required.

## Quick Start
1) Start services (optional if you have local DB/Redis/MinIO):
   ```bash
   docker compose up -d
   ```
   Default DB creds: `cloud_disk` / `cloud_disk` on `localhost:5432`. MinIO runs on `http://localhost:9000` (console `:9001`) with `minioadmin` / `minioadmin`.

2) Run the app:
   ```bash
   ./mvnw -pl application spring-boot:run
   ```
   Or build all modules: `./mvnw clean package`.

3) Check health / sample endpoint:
   - REST: `GET http://localhost:8080/users/hello`
   - Swagger UI: `http://localhost:8080/swagger-ui.html` (springdoc).

## Dev Tooling (fmt, lint, hooks)
- Format all Java + POMs: `npm run fmt` (Spotless + google-java-format 1.22.0).
- Check formatting only: `npm run fmt:check`.
- Lint: `npm run lint` (Checkstyle, Google style rules).
- Git hooks: Husky pre-commit runs `fmt` + `lint`; install via `npm install` (runs `npm run prepare` automatically).

## Configuration
Edit `application/src/main/resources/application.properties` for datasource/cache settings. To disable Compose auto-start, keep `spring.docker.compose.enabled=false`.

## Testing
```bash
./mvnw test
```
Add tests alongside implementation packages (e.g., `web-api/src/test/java/...`).

## Dependencies
Upgraded to Spring Boot 4.0.0 with current springdoc (2.6.0), MinIO client (8.5.12), and Hutool (5.8.32). Use the root `pom.xml` for future version bumps to keep modules aligned.

## MinIO / S3 bootstrap
- Toggle `storage.s3.bootstrap.enabled=true` in `application/src/main/resources/application.properties` (or via env var) to auto-create a `minio-default` S3 storage source using the MinIO credentials from Compose.
- Bucket name defaults to `cloud-disk` and will be created automatically if permitted.
