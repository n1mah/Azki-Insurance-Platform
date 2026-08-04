# Policy Service

Handles insurance product catalog and policy issuance for the Azki Insurance Platform. Trusts JWTs issued by `auth-service`; never issues tokens itself, only verifies them.

## 30-Second Overview

- `GET /api/policies/products` returns the available insurance products, backed by a Redis cache.
- `POST /api/policies` issues a new policy for the authenticated user against a chosen product.
- `GET /api/policies/{id}` returns a single policy by its ID.
- Every endpoint requires a valid JWT issued by `auth-service`; there are no public routes here.

## 2-Minute Quickstart

```bash
# From the project root
docker compose up -d mysql redis

cd policy-service
./mvnw spring-boot:run
```

The service listens on port `8082`. You need a valid JWT from `auth-service` (port `8081`) first:

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "demo_user", "password": "SecurePass123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

curl -X GET http://localhost:8082/api/policies/products \
  -H "Authorization: Bearer $TOKEN"
```

## API Reference

| Method | Path | Auth required | Description |
|---|---|---|---|
| `GET` | `/api/policies/products` | Yes | Lists available insurance products (cached) |
| `POST` | `/api/policies` | Yes | Issues a new policy for the authenticated user |
| `GET` | `/api/policies/{id}` | Yes | Retrieves a single policy by ID |
| `GET` | `/actuator/health` | No | Health check for Docker/monitoring |
| `GET` | `/actuator/prometheus` | No | Metrics endpoint for Prometheus |

### Error Responses

| Status | Cause |
|---|---|
| `403 Forbidden` | Missing or invalid JWT |
| `404 Not Found` | Requested product or policy does not exist |

## Package Structure

```
com.azki.policy
├── entity/       JPA entities (InsuranceProduct, Policy, PolicyStatus)
├── repository/   Spring Data JPA repositories
├── dto/          Request/response records for the API layer
├── exception/    Custom exceptions and the global exception handler
├── security/     JwtService (verification only) and JwtAuthenticationFilter
├── config/       Security filter chain and Redis cache configuration
├── service/      Business logic (PolicyService)
└── controller/   REST endpoints (PolicyController)
```

## Key Design Decisions

### `InsuranceProduct` Uses `Long`, `Policy` Uses `UUID`

`InsuranceProduct` is an internal catalog table; no other service ever references a product by ID, so a simple auto-incrementing `Long` is sufficient. `Policy`, on the other hand, is expected to be referenced by `claims-service` and `payment-service` in the future, so it uses a `UUID` to avoid collisions between IDs generated independently across services.

### `Policy.userId` Is a Plain `UUID`, Not a JPA Relation

Consistent with the database-per-service pattern: `User` lives in `auth_db`, a separate database that `policy_db` cannot join against. `userId` is stored as a plain field, and referential integrity across services is enforced at the application level.

### `Policy.product` Is a Real `@ManyToOne`

Unlike `userId`, `InsuranceProduct` lives in the same database (`policy_db`), so a genuine JPA relationship with a foreign key constraint is used here. This is a deliberate contrast: cross-database references use plain IDs, same-database references use real relations.

### `JwtService` Only Verifies, Never Issues

### JWT Verification Comes From a Shared Starter, Not a Local Copy

This service never logs a user in or registers one; it only needs to confirm that a token presented to it was legitimately issued by `auth-service`. `JwtService` and `JwtAuthenticationFilter` used to be duplicated verbatim between this service and `auth-service`. They now live in a separate library, `azki-security-spring-boot-starter`, published to a self-hosted Nexus repository and pulled in as a normal Maven dependency. Adding the dependency is enough; `SecurityAutoConfiguration` in the starter registers both beans automatically, no local `@Bean` definitions required. The signing secret must still match `auth-service`'s exactly, since HMAC verification is symmetric.

### Redis Cache Configured for JSON, Not Java Serialization

Spring's default cache serialization uses Java's built-in `Serializable` mechanism, which JPA entities intentionally do not implement (entities have proxies and lazy associations that don't serialize cleanly). `CacheConfig` explicitly configures `GenericJacksonJsonRedisSerializer` so cached values are stored as readable JSON instead, with a 30-minute TTL as a safety net against stale entries.

### No Role or Product Selection From the Client

`IssuePolicyRequest` only accepts a `productId`. The user ID comes from the verified JWT, never from the request body, closing an obvious spoofing path. The premium amount is always computed from the product's `basePremiumRate` in the database, never accepted from the client.

### Monetary Amounts Use a `Money` Value Object, Not Raw `BigDecimal`

`InsuranceProduct.basePremiumRate` and `Policy.premiumAmount` are `Money`, an `@Embeddable` value object pairing a `BigDecimal` amount with a currency string. A `Money` cannot be constructed with a null or negative amount, and its `equals()` compares by numeric value (`compareTo`) rather than `BigDecimal.equals()`, which would otherwise treat `100.0` and `100.00` as unequal due to differing scale. DTOs expose `amount` and `currency` as separate fields rather than serializing the `Money` type itself.

## Testing

The service is covered by 10 automated tests across two layers:

### Unit Tests (5 tests, no database, cache, or Spring context)

- **`PolicyServiceTest`** — issuing a policy with the correct premium and `ACTIVE` status, rejecting a non-existent product, listing a user's policies, retrieving a policy by ID, and rejecting a non-existent policy ID. `PolicyRepository` and `InsuranceProductRepository` are mocked via Mockito.

### Integration Tests (5 tests, real ephemeral MySQL and Redis via Testcontainers)

- **`PolicyApiIntegrationTest`** — exercises the full HTTP request path through `MockMvc`, a real MySQL 8.0 container, and a real Redis 7 container. Test JWTs are generated directly with the same secret and claim structure `auth-service` uses, since this service has no login endpoint of its own. Covers rejecting requests without a token, listing products with a valid token, issuing and retrieving a policy, and 404 responses for non-existent products and policies.

Run all tests:

```bash
./mvnw test
```

## Configuration

Key properties in `application.yaml`:

| Property | Purpose |
|---|---|
| `server.port` | `8082`, distinct from every other service in the platform |
| `spring.datasource.*` | Connection to the `policy_db` database and the `azki_app` user |
| `spring.data.redis.*` | Connection to the shared Redis instance |
| `spring.jpa.hibernate.ddl-auto` | `validate`; schema is owned by Flyway, not Hibernate |
| `jwt.secret` | Must match `auth-service`'s secret exactly; HMAC verification is symmetric |
| `management.endpoints.web.exposure.include` | Restricted to `health,prometheus` |

## Known Gotchas Encountered While Building This Service

- Redis's default cache serializer fails on JPA entities with `NotSerializableException`, since entities correctly don't implement `Serializable`. Fixed by configuring `GenericJacksonJsonRedisSerializer` explicitly.
- `GenericJacksonJsonRedisSerializer` (Jackson 3-based, used in Spring Data Redis 4.1) has no public constructor; it must be built via `GenericJacksonJsonRedisSerializer.builder().build()`.
- Testcontainers has no dedicated Redis module; a plain `GenericContainer` with the `redis:7-alpine` image is used instead.
- Manually inserting non-ASCII data (e.g. Persian text) via `docker exec ... mysql -e "..."` requires the `--default-character-set=utf8mb4` flag on the client, or the text gets double-encoded and corrupted, even though the column itself is correctly configured as `utf8mb4`.
- `GenericJacksonJsonRedisSerializer` (used for caching) has no public constructor in Spring Data Redis 4.1; it must be built via `.builder().build()`.
- Changing the type of a field shared across entities, DTOs, and tests (e.g. `BigDecimal` to `Money`) touches every consumer atomically; no subset of those files compiles on its own, so the resulting commit is necessarily large.