# Repository Guidelines

## Project Structure & Module Organization
- Maven multi-module layout: `domain` (entities and value objects), `repository` (JPA repositories), `web-api` (controllers/services), and `application` (Spring Boot runnable app and configs). Common resources live in `application/src/main/resources`.
- Root build files: `pom.xml` orchestrates modules; `compose.yaml` provisions Postgres, Redis, and MinIO for local dev. Scripts/hooks sit under `.husky` and `justfile`.

## Build, Test, and Development Commands
- Bootstrap dependencies: `npm install` (sets up Husky) and `./mvnw -v` to confirm wrapper/Java 17.
- Build all modules: `./mvnw clean verify`. Package without tests: `./mvnw -DskipTests package`.
- Run locally: `./mvnw -pl application spring-boot:run` (picks up `application` module). Backend jar lives in `application/target/`.
- Bring up services: `docker compose up -d` (uses defaults from `compose.yaml`).
- Format/lint: `npm run fmt` (Spotless apply) and `npm run lint` (Checkstyle check). CI-safe check: `npm run fmt:check`.

## Coding Style & Naming Conventions
- Java 17, Google Java Format via Spotless; Checkstyle uses `google_checks.xml`. Two-space indent, 100-char lines, imports ordered automatically.
- Packages under `org.superwindcloud.cloud_disk`. Classes use PascalCase, fields camelCase. JPA entities end with singular nouns (e.g., `FileItem`); repositories end with `Repository`.
- Favor Lombok for boilerplate but keep constructors visible when needed for frameworks.

## Testing Guidelines
- Test runner: `./mvnw test` (or `./mvnw -pl web-api test` per module). Add Spring MVC slice tests for controllers and repository integration tests against Postgres (use `@DataJpaTest` with testcontainers or local compose).
- Name tests `*Test.java`; structure given/when/then in method names. Aim for coverage on authentication, storage interactions (MinIO), and link shortener flows.

## Commit & Pull Request Guidelines
- Commits: short imperative subject (<72 chars), note affected module in scope when useful (e.g., `web-api: secure file upload`). Squash local fixups before pushing.
- PRs: include summary of change, test evidence/commands run, and any config changes (.env, compose, schema). Add screenshots or curl examples for new endpoints/Swagger changes. Link related issues and request reviewers touching the same module boundaries (domain ↔ repository ↔ web-api).

## Security & Configuration Tips
- Secrets: never commit real credentials; sample values live in `.env`/`compose.yaml`. Use environment overrides for OAuth clients and MinIO access keys.
- Validate inbound data with `spring-boot-starter-validation`; keep controllers thin and push business rules into services/domain objects.
