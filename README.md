# Azki Insurance Platform

A microservice-based insurance issuance and management system, built to practice and prepare for a Senior Java Developer technical interview at Azki. The stack closely mirrors the job posting's requirements: Spring Boot, MySQL, Redis, MongoDB, RabbitMQ, ELK, and Prometheus/Grafana.

## 30-Second Overview

Six independent services, each owning its own data, communicating through REST (sync) and RabbitMQ (async):

| Service | Responsibility | Status |
|---|---|---|
| `auth-service` | Authentication, JWT issuance | ✅ Complete and tested |
| `policy-service` | Policy issuance and lookup | ✅ Complete and tested |
| `claims-service` | Claims handling, optimistic locking | 🔲 Planned |
| `payment-service` | Payments, event consumer | 🔲 Planned |
| `notification-service` | SMS/email, fully async | 🔲 Planned |
| `gateway-service` | Single entry point, rate limiting | 🔲 Planned |

## 2-Minute Quickstart

Prerequisite: Docker Desktop installed and running.

```bash
# 1) Start the infrastructure
docker compose up -d mysql

# 2) Verify it's healthy
docker compose ps
# azki-mysql should show status "Up (healthy)"

# 3) Run auth-service
cd auth-service
./mvnw spring-boot:run
```

The service starts on port `8081`. Quick smoke test:

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "test_user", "password": "SecurePass123"}'
```

## Project Structure

```
Azki-Insurance-Platform/
├── docker-compose.yml       Shared infrastructure (MySQL, later Redis/RabbitMQ/...)
├── infra/
│   └── docker/
│       └── mysql-init/      Database and user creation scripts per service
├── auth-service/            Authentication service (complete)
└── ...                      Remaining services
```

## Architecture and Key Decisions

### Database-per-Service (Logical, Not Physical)

A single shared MySQL instance (`azki-mysql`) hosts multiple separate databases (`auth_db`, and soon `policy_db`, `claims_db`, `payment_db`). Each service connects with its own dedicated user, whose privileges are restricted (via `GRANT`) to that service's database only. No service queries another service's tables directly.

### Inter-Service Communication

- **Sync (REST)** for requests that need an immediate response.
- **Async (RabbitMQ, topic exchange)** for events such as `policy.issued`, with idempotent consumers and a dead letter queue for messages that repeatedly fail processing.

### Choreography-Based Saga

There is no central orchestrator. Each service reacts independently to the events it consumes.

### Race Condition Handling in Claims Service (Planned)

Optimistic locking via `@Version`, as opposed to the pessimistic locking (`SELECT FOR UPDATE SKIP LOCKED`) used in a previous project (the reservation system). This is a deliberate choice to be able to compare and discuss both approaches in the interview.

## Testing Strategy

Each service is covered at two levels:

- **Unit tests** (JUnit 5 + Mockito) for business logic, without a database or Spring context.
- **Integration tests** (MockMvc + Testcontainers) against a real, ephemeral MySQL instance, covering the full path from HTTP request through controller, service, and database.

## Notable Spring Boot 4 Changes (Documented for Interview Reference)

This project uses Spring Boot 4.1.0, which introduces several breaking architectural changes compared to version 3:

- Starters have been modularized (`spring-boot-starter-web` → `spring-boot-starter-webmvc`).
- Each primary starter now has a dedicated test starter (`spring-boot-starter-webmvc-test`).
- Auto-configuration for third-party libraries (e.g., Flyway) is no longer triggered by adding the raw dependency alone; it now requires an explicit starter (`spring-boot-starter-flyway`).
- Jackson has been upgraded to version 3; the package has moved from `com.fasterxml.jackson` to `tools.jackson`.
- `@AutoConfigureMockMvc` has moved to the `org.springframework.boot.webmvc.test.autoconfigure` module.
- Null-safety annotations have moved from `org.springframework.lang` to `org.jspecify.annotations`.

## Repository

`https://github.com/n1mah/Azki-Insurance-Platform`

All commits follow the Conventional Commits specification (`feat:`, `fix:`, `test:`, `chore:`), with each logical change committed separately.