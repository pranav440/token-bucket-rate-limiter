# Token Bucket Rate Limiter 🚦

> An **enterprise-grade Token Bucket Rate Limiter** built with Spring Boot 4, JPA, MySQL, and SpringDoc OpenAPI. Implements the classic token bucket algorithm with pessimistic locking for thread-safe, concurrent rate enforcement.

---

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Folder Structure](#folder-structure)
- [How to Run](#how-to-run)
- [API Endpoints](#api-endpoints)
- [Sample Requests & Responses](#sample-requests--responses)
- [Swagger UI](#swagger-ui)
- [Future Improvements](#future-improvements)
- [Learning Outcomes](#learning-outcomes)
- [Screenshots](#screenshots)
- [License](#license)
- [Author](#author)

---

## ✨ Features

| Feature | Detail |
|---|---|
| **Token Bucket Algorithm** | Per-client token refill based on elapsed wall-clock time |
| **Pessimistic Locking** | `SELECT … FOR UPDATE` on `bucket_states` prevents race conditions |
| **Client CRUD** | Full create / read / update / enable / disable / delete lifecycle |
| **Auto-generated Credentials** | Unique `clientId` and `apiKey` provisioned on registration |
| **Bean Validation** | Jakarta Validation on all request DTOs |
| **Structured Error Responses** | Consistent `ErrorResponse` / `ValidationErrorResponse` payloads |
| **OpenAPI 3 / Swagger UI** | Interactive API documentation at `/swagger-ui/index.html` |
| **Optimistic Versioning** | `@Version` on `Client` entity prevents lost-update anomalies |
| **Structured Logging** | SLF4J + Logback with per-layer log levels |
| **Environment-aware Config** | DB credentials externalisable via env variables |

---

## 🏛️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                   HTTP Clients                      │
└─────────────────┬───────────────────────────────────┘
                  │  REST (JSON)
┌─────────────────▼───────────────────────────────────┐
│              Controller Layer                        │
│  ClientController   │   RateLimiterController        │
└─────────────────┬───────────────────────────────────┘
                  │  Service interfaces
┌─────────────────▼───────────────────────────────────┐
│               Service Layer                          │
│  ClientServiceImpl  │  RateLimiterServiceImpl         │
│              (Token Bucket Algorithm)                 │
└─────────────────┬───────────────────────────────────┘
                  │  JPA Repositories
┌─────────────────▼───────────────────────────────────┐
│             Repository Layer                         │
│  ClientRepository   │  BucketStateRepository         │
│                 (Pessimistic Lock)                   │
└─────────────────┬───────────────────────────────────┘
                  │  JDBC
┌─────────────────▼───────────────────────────────────┐
│                  MySQL Database                      │
│     clients table  │  bucket_states table            │
└─────────────────────────────────────────────────────┘
```

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for detailed layer and sequence diagrams.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.1 |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL 8 |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Validation | Jakarta Validation (Bean Validation 3) |
| Lombok | Boilerplate reduction |
| Build | Maven |
| Testing | JUnit 5, Mockito, AssertJ |

---

## 📂 Folder Structure

```
src/main/java/com/pranav/token_bucket_rate_limiter/
│
├── config/
│   └── OpenApiConfig.java          # Swagger / OpenAPI bean
│
├── controller/
│   ├── ClientController.java        # Client CRUD endpoints
│   └── RateLimiterController.java   # Rate-limit check endpoint
│
├── dto/
│   ├── request/
│   │   ├── CreateClientRequest.java
│   │   ├── UpdateClientRequest.java
│   │   └── RateLimitRequest.java
│   └── response/
│       ├── ClientResponse.java
│       ├── RateLimitResponse.java
│       ├── ErrorResponse.java
│       └── ValidationErrorResponse.java
│
├── entity/
│   ├── Client.java                  # API client entity
│   └── BucketState.java             # Token bucket state entity
│
├── enums/
│   ├── AlgorithmType.java
│   └── ClientStatus.java
│
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   ├── ClientNotFoundException.java
│   ├── DuplicateClientException.java
│   ├── InvalidApiKeyException.java
│   └── BucketNotFoundException.java
│
├── mapper/
│   └── ClientMapper.java            # Entity ↔ DTO conversions
│
├── repository/
│   ├── ClientRepository.java
│   └── BucketStateRepository.java   # Pessimistic write lock
│
├── service/
│   ├── interfaces/
│   │   ├── ClientService.java
│   │   └── RateLimiterService.java
│   └── impl/
│       ├── ClientServiceImpl.java
│       └── RateLimiterServiceImpl.java
│
└── util/
    ├── ApiKeyGenerator.java         # UUID-based API key generator
    └── ClientIdGenerator.java       # Short human-readable ID generator
```

---

## 🚀 How to Run

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8 running locally

### 1. Create the database

```sql
CREATE DATABASE token_bucket_db;
```

### 2. Configure credentials

Edit `src/main/resources/application.properties` or set environment variables:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The server starts on **http://localhost:8080**

### 4. Run tests

```bash
./mvnw test
```

---

## 📡 API Endpoints

### Client Management — `/api/v1/clients`

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/api/v1/clients` | Register new client | `201 Created` |
| `GET` | `/api/v1/clients` | Get all clients | `200 OK` |
| `GET` | `/api/v1/clients/{clientId}` | Get client by ID | `200 OK` |
| `PUT` | `/api/v1/clients/{clientId}` | Update client | `200 OK` |
| `PATCH` | `/api/v1/clients/{clientId}/enable` | Enable client | `204 No Content` |
| `PATCH` | `/api/v1/clients/{clientId}/disable` | Disable client | `204 No Content` |
| `DELETE` | `/api/v1/clients/{clientId}` | Delete client | `204 No Content` |

### Rate Limiter — `/api/v1/rate-limit`

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/api/v1/rate-limit` | Check rate limit | `200` / `429` |

---

## 📨 Sample Requests & Responses

### Register a Client

**Request:**
```http
POST /api/v1/clients
Content-Type: application/json

{
  "clientName": "My Mobile App",
  "ownerEmail": "dev@myapp.com",
  "requestsPerSecond": 5,
  "burstCapacity": 20,
  "algorithmType": "TOKEN_BUCKET",
  "description": "Main mobile API client"
}
```

**Response `201 Created`:**
```json
{
  "clientId": "CLIENT_A1B2C3D4",
  "clientName": "My Mobile App",
  "apiKey": "tb_live_9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c",
  "requestsPerSecond": 5,
  "burstCapacity": 20,
  "algorithmType": "TOKEN_BUCKET",
  "status": "ACTIVE",
  "ownerEmail": "dev@myapp.com",
  "description": "Main mobile API client"
}
```

---

### Check Rate Limit

**Request:**
```http
POST /api/v1/rate-limit
Content-Type: application/json

{
  "apiKey": "tb_live_9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c"
}
```

**Response `200 OK` (allowed):**
```json
{
  "allowed": true,
  "remainingTokens": 19,
  "retryAfter": 0,
  "timestamp": "2026-07-11T19:23:28"
}
```

**Response `429 Too Many Requests` (denied):**
```json
{
  "allowed": false,
  "remainingTokens": 0,
  "retryAfter": 200,
  "timestamp": "2026-07-11T19:23:28"
}
```

---

### Error Responses

**`400 Bad Request` (Validation):**
```json
{
  "timestamp": "2026-07-11T19:23:28",
  "status": 400,
  "errors": {
    "clientName": "Client name is required",
    "ownerEmail": "Invalid email format"
  }
}
```

**`404 Not Found`:**
```json
{
  "timestamp": "2026-07-11T19:23:28",
  "status": 404,
  "message": "Client not found: CLIENT_XXXX"
}
```

**`409 Conflict`:**
```json
{
  "timestamp": "2026-07-11T19:23:28",
  "status": 409,
  "message": "A client with that name already exists."
}
```

---

## 📖 Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON spec:
```
http://localhost:8080/v3/api-docs
```

---

## 🔮 Future Improvements

| Improvement | Description |
|---|---|
| **Spring Security** | Protect management endpoints with JWT or API-key authentication |
| **Redis-backed Buckets** | Replace MySQL pessimistic lock with atomic Redis operations for horizontal scalability |
| **Sliding Window Algorithm** | Implement `SLIDING_WINDOW` algorithm type (already in enum) |
| **Rate-limit Headers** | Return `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After` headers |
| **Admin Dashboard** | React/Angular UI for real-time bucket monitoring |
| **Metrics** | Micrometer + Prometheus + Grafana integration |
| **Caching** | Cache active client lookups with Caffeine/Redis |
| **Audit Log** | Persist rate-limit decisions to an audit table |
| **Multi-tenancy** | Namespace buckets by organisation |

---

## 🎓 Learning Outcomes

Building this project teaches:

1. **Token Bucket Algorithm** — How token refill rate and burst capacity produce two distinct throttling behaviours
2. **Pessimistic Locking in JPA** — `@Lock(PESSIMISTIC_WRITE)` translates to `SELECT … FOR UPDATE`, preventing the lost-update problem under concurrent requests
3. **Optimistic vs Pessimistic Locking** — When to use `@Version` (optimistic) vs `@Lock` (pessimistic)
4. **JPA Lifecycle Hooks** — `@PrePersist`/`@PreUpdate` for audit timestamps without service-layer duplication
5. **Spring Transactions** — `@Transactional` propagation, `readOnly = true` optimisation
6. **Global Exception Handling** — `@RestControllerAdvice` for centralized, consistent error responses
7. **DTO Pattern** — Clean separation between API contracts and internal entity models
8. **Mapper Pattern** — Static utility class for entity ↔ DTO conversion
9. **OpenAPI 3** — SpringDoc annotations for self-documenting APIs
10. **Unit Testing** — Mockito, AssertJ, and @Nested test organisation

---

## 📸 Screenshots

| | |
|---|---|
| ![Swagger UI](<img width="796" height="536" alt="image" src="https://github.com/user-attachments/assets/851da800-a46e-4dd3-a0e2-f3442ee9aaff" />
) | ![Rate Limit Check](<img width="959" height="599" alt="image" src="https://github.com/user-attachments/assets/579a5d65-9645-4309-ac21-3cb1fa961698" />
) |
| *Swagger UI — API Explorer* | *Rate limit check — 200 response* |

> Screenshots to be added after deployment.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 👤 Author

**Pranav Ganorkar**

- GitHub: [@pranavganorkar](https://github.com/pranav440)
- Email: pranav@example.com

---

*Built as a learning project to deeply understand rate limiting algorithms, JPA concurrency control, and enterprise Spring Boot patterns.*
