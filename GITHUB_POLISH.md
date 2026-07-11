# GitHub Polish Guide

## Repository Setup

### Repository Description (GitHub "About" section)

```
Enterprise Token Bucket Rate Limiter — Spring Boot 4 · JPA · MySQL · Pessimistic Locking · OpenAPI 3
```

### Topics / Tags

```
java  spring-boot  rate-limiting  token-bucket  rest-api  jpa  mysql
spring-data-jpa  swagger  openapi  pessimistic-locking  backend
rate-limiter  java17  spring-boot-4
```

---

## .gitignore Review

Current `.gitignore` is Spring Initializr standard. The following additions are recommended:

```gitignore
# ===========================
# Additional entries to add
# ===========================

# Environment / secrets
.env
*.env

# Build artifacts
*.log
build.log
build_utf8.log
compile_clean.txt
compile_out.txt

# macOS
.DS_Store

# Windows
Thumbs.db
desktop.ini

# Application secrets (never commit real credentials)
src/main/resources/application-prod.properties
src/main/resources/application-local.properties
```

---

## Professional Commit History Suggestion

Below is a suggested commit sequence representing a realistic, professional Git history for this project:

```
git log --oneline

a1b2c3d (HEAD -> main, tag: v1.0.0) docs: add comprehensive README and ARCHITECTURE.md
b2c3d4e test: add unit tests for RateLimiterServiceImpl and ClientServiceImpl
c3d4e5f feat: add Swagger/OpenAPI 3 documentation with full @ApiResponses
d4e5f6a fix: always persist BucketState after rate-limit check (both allowed and denied paths)
e5f6a7b fix: remove non-existent spring-boot-starter-data-jpa-test dependency from pom.xml
f6a7b8c fix: remove stray Hibernate internal import from ClientService interface
a7b8c9d chore: remove unused imports (ToString, RequiredArgsConstructor, ClientService) from repositories
b8c9d0e style: fix inconsistent indentation in ClientController
c9d0e1f feat: add utility class pattern (final + private constructor) to mapper and generators
d0e1f2a feat: add @Column(nullable=false) constraints and JavaDoc to entity classes
e1f2a3b feat: add logging to GlobalExceptionHandler and extract buildErrorResponse() helper
f2a3b4c feat: externalize DB credentials using ${DB_USERNAME:root} Spring expression
a3b4c5d chore: add Hibernate dialect property to application.properties
b4c5d6e feat: implement pessimistic locking in BucketStateRepository
c5d6e7f feat: implement Token Bucket algorithm in RateLimiterServiceImpl
d6e7f8a feat: implement ClientServiceImpl with full CRUD and bucket provisioning
e7f8a9b feat: implement GlobalExceptionHandler with structured error responses
f8a9b0c feat: add domain exceptions (ClientNotFoundException, DuplicateClientException, etc.)
a9b0c1d feat: add ClientMapper with static toEntity and toResponse methods
b0c1d2e feat: add request/response DTOs with Jakarta Validation
c1d2e3f feat: add Client and BucketState JPA entities with lifecycle hooks
d2e3f4a feat: add ClientRepository and BucketStateRepository
e3f4a5b feat: add AlgorithmType and ClientStatus enums
f4a5b6c feat: add API key and client ID generator utilities
a5b6c7d chore: initialize Spring Boot project structure with Maven
```

---

## Release Notes — v1.0.0

**Tag:** `v1.0.0`  
**Date:** 2026-07-11  
**Release Type:** Initial Release

---

### 🎉 What's New

This is the first production-quality release of the Token Bucket Rate Limiter Service.

#### Core Features
- **Token Bucket Algorithm** — Per-client rate limiting with configurable `requestsPerSecond` and `burstCapacity`
- **Pessimistic Write Lock** — `SELECT ... FOR UPDATE` on `bucket_states` prevents race conditions under concurrent load
- **Client Management API** — Full CRUD: register, retrieve, update, enable, disable, delete
- **Auto-provisioned Credentials** — Each client receives a unique `clientId` and `apiKey` at registration
- **Burst Support** — Clients can absorb burst traffic up to `burstCapacity` tokens before throttling

#### API & Documentation
- **RESTful JSON API** — HTTP/1.1 compliant with appropriate status codes (200, 201, 204, 400, 401, 404, 409, 429)
- **Swagger UI** — Interactive documentation at `/swagger-ui/index.html`
- **OpenAPI 3 Spec** — Machine-readable spec at `/v3/api-docs`

#### Quality & Reliability
- **Jakarta Validation** — Input validation on all request DTOs
- **Global Exception Handler** — Consistent error response shapes across all endpoints
- **Structured Logging** — SLF4J + Logback with per-layer log levels
- **Optimistic Versioning** — `@Version` on Client entity prevents lost-update anomalies
- **JPA Lifecycle Hooks** — `createdAt`/`updatedAt` managed automatically

#### Testing
- **Unit Tests** — JUnit 5 + Mockito + AssertJ
  - `ClientServiceImplTest` — 13 test cases
  - `RateLimiterServiceImplTest` — 12 test cases
  - Total: 25 focused unit tests

---

### 🐛 Known Limitations

- `SLIDING_WINDOW` algorithm type is defined in enum but not yet implemented
- No authentication on management endpoints (client registration is open)
- DB credentials must be manually configured (no Docker/env auto-discovery)

---

### 📦 How to Deploy

```bash
# 1. Create database
mysql -u root -p -e "CREATE DATABASE token_bucket_db;"

# 2. Set credentials
export DB_USERNAME=root
export DB_PASSWORD=yourpassword

# 3. Run
./mvnw spring-boot:run
```

---

### 📋 Compatibility

| Component | Version |
|---|---|
| Java | 17 |
| Spring Boot | 4.1.0 |
| MySQL | 8.x |
| SpringDoc OpenAPI | 2.8.9 |

---

*For architecture details, see [`ARCHITECTURE.md`](ARCHITECTURE.md).*
