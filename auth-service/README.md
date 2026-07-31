# Auth Service

Handles user registration, authentication, and JWT issuance for the Azki Insurance Platform. This is the entry point of the authentication flow; every other service in the platform trusts the JWTs this service issues, without needing to call back to it.

## 30-Second Overview

- `POST /api/auth/register` creates a new user with the `CUSTOMER` role and returns a JWT.
- `POST /api/auth/login` authenticates an existing user and returns a fresh JWT.
- Passwords are hashed with BCrypt and never stored or returned in plain text.
- The service is stateless: no server-side sessions, only signed JWTs.

## 2-Minute Quickstart

```bash
# From the project root
docker compose up -d mysql

cd auth-service
./mvnw spring-boot:run
```

The service listens on port `8081`.

```bash
# Register a new user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "nima_test", "password": "SecurePass123"}'

# Log in with the same credentials
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "nima_test", "password": "SecurePass123"}'
```

Both endpoints return:

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer"
}
```

## API Reference

| Method | Path | Auth required | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | No | Creates a new user, returns a JWT |
| `POST` | `/api/auth/login` | No | Authenticates a user, returns a JWT |
| `GET` | `/actuator/health` | No | Health check for Docker/monitoring |
| `GET` | `/actuator/prometheus` | No | Metrics endpoint for Prometheus |

### Error Responses

| Status | Cause |
|---|---|
| `400 Bad Request` | Validation failure (e.g., blank username, password shorter than 8 characters) |
| `401 Unauthorized` | Invalid username or password during login |
| `409 Conflict` | Username already exists during registration |

## Package Structure

```
com.azki.auth
├── entity/       JPA entities (User, UserRole)
├── repository/   Spring Data JPA repositories
├── dto/          Request/response records for the API layer
├── exception/    Custom exceptions and the global exception handler
├── security/     JwtService and JwtAuthenticationFilter
├── config/       Security configuration (password encoder, filter chain)
├── service/      Business logic (UserService)
└── controller/   REST endpoints (AuthController)
```

## Key Design Decisions

### UUID Primary Keys

`User.id` is a `UUID`, not an auto-incrementing integer. In a microservice architecture, identifiers frequently cross service boundaries (e.g., a `Policy` references a `userId`). UUIDs avoid collisions between IDs generated independently by different services, which sequential integers cannot guarantee.

### No Cross-Database Foreign Keys

Other services (e.g., `policy-service`) store a user's ID as a plain `UUID` field, not as a database-level foreign key, because the `User` table lives in a separate database (`auth_db`) that they cannot join against. Referential integrity for these references is enforced at the application level, not the database level. This is an explicit and necessary trade-off of the database-per-service pattern.

### Role Stored as `STRING`, Not `ORDINAL`

`UserRole` is persisted as `@Enumerated(EnumType.STRING)`. If the enum's declaration order ever changes, `ORDINAL` storage would silently corrupt existing data by reassigning roles to the wrong numeric values. `STRING` storage is immune to that failure mode, at a small storage cost.

### User Entity Is Immutable After Construction

`User` exposes no setters. Its state is fixed by the constructor at creation time. Any future state change (e.g., changing a password) is intended to go through an explicit, intention-revealing method rather than a generic setter, keeping the entity harder to misuse.

### Role Assignment Happens Server-Side Only

`RegisterRequest` has no `role` field. A user cannot request to be created as `ADMIN`; the service always assigns `CUSTOMER` on registration. This closes an obvious privilege-escalation path at the API boundary rather than relying on validation to catch it later.

### Ambiguous Error Message for Invalid Credentials

Both "user not found" and "wrong password" return the same message: `invalid username or password`. Returning distinct messages would let an attacker enumerate which usernames exist in the system, which is a well-known information-disclosure risk in authentication endpoints.

### Stateless Security (No Server-Side Sessions)

`SessionCreationPolicy.STATELESS` is set explicitly. Every request must carry its own valid JWT; the server holds no session state between requests. This aligns with the horizontal scalability goals of a microservice architecture, where any instance of the service should be able to handle any request.

### Schema Managed by Flyway, Not Hibernate

`spring.jpa.hibernate.ddl-auto` is set to `validate`, not `update` or `create`. Hibernate only checks that the entities match the existing schema; it never creates or alters tables. All schema changes are explicit, versioned SQL migration files under `src/main/resources/db/migration`, managed by Flyway. This is the standard, safe approach for a production system, where uncontrolled automatic schema changes are a common source of incidents.

## Testing

The service is covered by 14 automated tests across two layers:

### Unit Tests (9 tests, no database or Spring context)

- **`UserServiceTest`** (5 tests) — registration and authentication logic, with `UserRepository` and `PasswordEncoder` mocked via Mockito. Covers successful registration, duplicate username rejection, successful authentication, wrong password rejection, and non-existent user rejection.
- **`JwtServiceTest`** (4 tests) — token generation and validation against a real (test-only) secret, with no mocking, since `JwtService` has no external dependencies. Covers token content, validity of a freshly generated token, rejection of a tampered token, and rejection of a malformed token.

### Integration Tests (5 tests, real ephemeral MySQL via Testcontainers)

- **`AuthApiIntegrationTest`** — exercises the full HTTP request path (`MockMvc` → `AuthController` → `UserService` → `UserRepository` → a real MySQL 8.0 container). Testcontainers starts a fresh MySQL instance for the test run, and Flyway runs the same migration used in production against it. Covers successful registration, duplicate username conflict, validation failure on blank username, successful login after registration, and rejection of a wrong password.

Run all tests:

```bash
./mvnw test
```

Run a single test class:

```bash
./mvnw test -Dtest=UserServiceTest
```

## Configuration

Key properties in `application.yaml`:

| Property | Purpose |
|---|---|
| `server.port` | `8081`, distinct from every other service in the platform |
| `spring.datasource.*` | Connection to the `auth_db` database and the `azki_app` user |
| `spring.jpa.hibernate.ddl-auto` | `validate`; schema is owned by Flyway, not Hibernate |
| `jwt.secret` | HMAC signing key; defaults to a development-only value, must come from an environment variable in production |
| `jwt.expiration-ms` | Token lifetime, currently `3600000` (1 hour) |
| `management.endpoints.web.exposure.include` | Restricted to `health,prometheus`; no other actuator endpoints are exposed |

## Known Spring Boot 4 Gotchas Encountered While Building This Service

- Raw `flyway-core`/`flyway-mysql` dependencies are not enough to trigger Flyway auto-configuration; `spring-boot-starter-flyway` must also be added explicitly.
- `@AutoConfigureMockMvc` now lives in `org.springframework.boot.webmvc.test.autoconfigure`, not `org.springframework.boot.test.autoconfigure.web.servlet`.
- Jackson's `ObjectMapper` is now imported from `tools.jackson.databind`, not `com.fasterxml.jackson.databind`.
- `org.springframework.lang.NonNull` is deprecated; use `org.jspecify.annotations.NonNull` instead.