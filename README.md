# Cloud Disk

Spring Boot 4 multi-module template for an online cloud storage service. Modules split into `domain`, `repository`, `web-api`, and `application`, with OpenAPI, MinIO client, OAuth2 login, Redis caching, and Postgres persistence.

## Prerequisites
- Java 17+, Maven Wrapper (`./mvnw`/`mvnw.cmd`), Node 18+ (for Husky hooks), Docker/Docker Compose.
- Running services: Postgres, Redis, MinIO. A default compose stack is provided.

## Quick Start
```bash
npm install                     # installs husky hooks
docker compose up -d            # start Postgres/Redis/MinIO
./mvnw clean verify             # build and run tests
./mvnw -pl application spring-boot:run  # start the app
```
Windows users can substitute `mvnw.cmd` for `./mvnw`.

## Project Structure
- `pom.xml` root aggregator and shared dependency management (Spotless, Checkstyle).
- `domain/`: JPA entities/value objects (e.g., `FileItem`, `StorageSource`, `User`).
- `repository/`: Spring Data JPA repositories.
- `web-api/`: Controllers/services, storage adapters, API docs.
- `application/`: Boot app wiring, security/OAuth2, configs, resources.
- `compose.yaml`: Local Postgres/Redis/MinIO.
- `.husky/`, `package.json`: Git hooks and format/lint scripts.

## Build, Test, and Format
- Full build: `./mvnw clean verify`
- Module-scoped: `./mvnw -pl web-api test` (swap module as needed)
- Format: `npm run fmt` (Spotless apply), check only: `npm run fmt:check`
- Lint: `npm run lint` (Checkstyle). Non-zero exit indicates style issues.

## Running Locally
- App: `./mvnw -pl application spring-boot:run`
- Jar: `java -jar application/target/application-*.jar` after packaging.
- Services: `docker compose ps` to confirm Postgres/Redis/MinIO are healthy. Default ports: 5432, 6379, 9000/9001.

## Configuration
- Copy `.env` as needed; keep secrets out of VCS. Override DB/MinIO/OAuth credentials via environment variables or Spring profiles.
- Jackson and security are preconfigured in `application` module; adjust in `application/src/main/java/org/superwindcloud/cloud_disk/*Config.java`.

## Contribution Notes
- Follow Google Java Format and Checkstyle (CI-failing). Run format before commits.
- Commit messages: short imperative, add module scope when useful (e.g., `web-api: add upload policy`).
- Include test evidence and any API examples/screenshots in PR descriptions; link issues and note config or schema changes.***
