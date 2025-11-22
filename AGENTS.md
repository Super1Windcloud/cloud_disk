# Repository Guidelines

## Project Structure & Module Organization
- Java sources live in `src/main/java/org/superwindcloud/cloud_disk`; entrypoint is `CloudDiskApplication`.
- Views and config are under `src/main/resources` (`templates/` for Thymeleaf, `static/` for assets, `application.properties` for runtime settings).
- Tests mirror the main package in `src/test/java/org/superwindcloud/cloud_disk`.
- `compose.yaml` defines local Postgres and Redis services; Spring Boot’s Docker Compose integration is currently disabled via `spring.docker.compose.enabled=false`.

## Build, Test, and Development Commands
- `./mvnw spring-boot:run` — start the app with local configuration (expects Postgres on `localhost:5432` and Redis on `6379`).
- `./mvnw test` — run the test suite.
- `./mvnw package` — build the executable JAR under `target/`.
- `docker compose up -d` — bring up Postgres/Redis as defined in `compose.yaml` (enable Compose in properties if you want Spring Boot to manage it).

## Coding Style & Naming Conventions
- Java 17; prefer 4-space indentation and trailing newline.
- Classes/interfaces use PascalCase; methods/fields use camelCase; constants are UPPER_SNAKE_CASE.
- Keep controllers/services/repositories annotated (`@RestController`, `@Service`, `@Repository`) and colocated by feature when added.
- Favor Lombok for boilerplate, but keep constructors explicit when clarity helps testing.
- Externalize env-specific settings in `application.properties` or profiles; avoid hardcoding secrets.

## Testing Guidelines
- Use Spring Boot test starters (JUnit/MockMvc) already on the classpath.
- Name test classes with `*Tests` and keep them parallel to the source package.
- For data-dependent tests, seed via SQL/data builders instead of depending on existing databases; prefer `@DataJpaTest` and `@SpringBootTest` only when needed.
- Run `./mvnw test` before opening a PR; include any new integration tests when touching persistence or security flows.

## Security & Configuration Tips
- Default credentials expect a Postgres role/db `cloud_disk` with password `cloud_disk`; adjust `spring.datasource.*` as needed.
- If running without Docker, ensure local Postgres/Redis match the ports in `compose.yaml`.
- Do not commit real secrets or tokens; use environment variables or a local profile file excluded from VCS.

## Commit & Pull Request Guidelines
- Use concise, imperative commit messages (e.g., “Add user registration endpoint”); group logical changes together.
- PRs should describe intent, list key changes, and note testing performed (`./mvnw test`, manual scenarios).
- Link related issues/tickets when applicable; include screenshots for UI-affecting changes.
