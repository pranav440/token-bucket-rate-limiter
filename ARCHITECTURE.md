# Architecture Documentation

## Token Bucket Rate Limiter Service

**Version:** 1.0.0  
**Author:** Pranav Ganorkar  
**Stack:** Spring Boot 4 · Java 17 · MySQL 8 · JPA/Hibernate

---

## 1. Layer Diagram

```
┌───────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                          │
│                                                                   │
│  ┌──────────────────────┐   ┌─────────────────────────────────┐  │
│  │   ClientController   │   │    RateLimiterController         │  │
│  │  /api/v1/clients     │   │   /api/v1/rate-limit             │  │
│  └──────────┬───────────┘   └───────────────┬─────────────────┘  │
│             │ @Valid + DTO                   │ @Valid + DTO        │
└─────────────┼───────────────────────────────┼────────────────────┘
              │                               │
┌─────────────▼───────────────────────────────▼────────────────────┐
│                        Service Layer                               │
│                                                                   │
│  ┌──────────────────────┐   ┌─────────────────────────────────┐  │
│  │  ClientServiceImpl   │   │   RateLimiterServiceImpl         │  │
│  │  @Transactional      │   │   @Transactional                 │  │
│  │  Client CRUD logic   │   │   Token Bucket Algorithm         │  │
│  └──────────┬───────────┘   └───────────────┬─────────────────┘  │
│             │                               │                     │
└─────────────┼───────────────────────────────┼────────────────────┘
              │                               │
┌─────────────▼───────────────────────────────▼────────────────────┐
│                       Repository Layer                             │
│                                                                   │
│  ┌──────────────────────┐   ┌─────────────────────────────────┐  │
│  │   ClientRepository   │   │   BucketStateRepository          │  │
│  │   JpaRepository      │   │   @Lock(PESSIMISTIC_WRITE)       │  │
│  └──────────┬───────────┘   └───────────────┬─────────────────┘  │
│             │                               │                     │
└─────────────┼───────────────────────────────┼────────────────────┘
              │          JDBC / JPA            │
┌─────────────▼───────────────────────────────▼────────────────────┐
│                        MySQL Database                              │
│                                                                   │
│   ┌────────────────────┐     ┌─────────────────────────────┐     │
│   │     clients        │     │       bucket_states          │     │
│   │  PK: id (BIGINT)   │◄────│  FK: client_id (BIGINT)     │     │
│   │  UQ: clientId      │ 1:1 │  availableTokens (INT)       │     │
│   │  UQ: clientName    │     │  lastRefillTime (DATETIME)   │     │
│   │  UQ: apiKey        │     └─────────────────────────────┘     │
│   │  requestsPerSec    │                                          │
│   │  burstCapacity     │                                          │
│   │  status (ENUM)     │                                          │
│   │  @Version (BIGINT) │                                          │
│   └────────────────────┘                                          │
└───────────────────────────────────────────────────────────────────┘

Cross-cutting: GlobalExceptionHandler · Lombok · SpringDoc OpenAPI
```

---

## 2. Entity Relationship

```
┌──────────────────────────────────────────────────────────────┐
│  Client                                                       │
│  ─────────────────────────────────────────────────────────── │
│  PK  id              BIGINT AUTO_INCREMENT                   │
│  UQ  clientId        VARCHAR  (e.g. CLIENT_A1B2C3D4)         │
│  UQ  clientName      VARCHAR                                  │
│  UQ  apiKey          VARCHAR  (e.g. tb_live_xxxxxxxx)        │
│      requestsPerSec  INT NOT NULL                             │
│      burstCapacity   INT NOT NULL                             │
│      algorithmType   ENUM('TOKEN_BUCKET','SLIDING_WINDOW')   │
│      status          ENUM('ACTIVE','INACTIVE')                │
│      description     VARCHAR(500)                             │
│      ownerEmail      VARCHAR NOT NULL                         │
│      createdAt       DATETIME NOT NULL (immutable)            │
│      updatedAt       DATETIME NOT NULL (@PreUpdate)           │
│      version         BIGINT  (@Version — optimistic lock)    │
└──────────────────────────────┬───────────────────────────────┘
                               │ OneToOne (CASCADE ALL,
                               │           orphanRemoval=true)
                               │ mapped by "client" field
┌──────────────────────────────▼───────────────────────────────┐
│  BucketState                                                  │
│  ─────────────────────────────────────────────────────────── │
│  PK  id               BIGINT AUTO_INCREMENT                  │
│  FK  client_id        BIGINT REFERENCES clients(id)          │
│      availableTokens  INT NOT NULL                            │
│      lastRefillTime   DATETIME NOT NULL                       │
└───────────────────────────────────────────────────────────────┘
```

**Notes:**
- `Client.bucketState` is `FetchType.LAZY` — only loaded when directly accessed.
- `BucketState.client` is `FetchType.LAZY` with a `@JoinColumn(name="client_id")`.
- Deletion of a `Client` cascades to its `BucketState` via `CascadeType.ALL + orphanRemoval=true`.
- `@Version` on `Client` provides optimistic locking for concurrent update operations.

---

## 3. Request Flow — Client Registration

```
POST /api/v1/clients
        │
        ▼
ClientController.registerClient(@Valid @RequestBody)
        │
        │── Validation passes (Jakarta Validation)
        ▼
ClientServiceImpl.registerClient(request)
        │
        ├── clientRepository.existsByClientName()
        │       └─ TRUE  → throw DuplicateClientException (409)
        │
        ├── ClientMapper.toEntity(request)
        │       ├── ClientIdGenerator.generate()  → "CLIENT_XXXXXXXX"
        │       ├── ApiKeyGenerator.generate()    → "tb_live_xxxxxxxx"
        │       └── status = ACTIVE
        │
        ├── BucketState.builder()
        │       ├── availableTokens = burstCapacity
        │       └── lastRefillTime  = now()
        │
        ├── client.setBucketState(bucketState)
        │
        ├── clientRepository.save(client)
        │       └── @PrePersist: createdAt = updatedAt = now()
        │           JPA cascades save to BucketState
        │
        └── ClientMapper.toResponse(savedClient)
                └─ 201 Created → ClientResponse JSON
```

---

## 4. Request Flow — Rate Limit Check

```
POST /api/v1/rate-limit
        │
        ▼
RateLimiterController.checkRateLimit(@Valid @RequestBody)
        │
        ▼
RateLimiterServiceImpl.checkRateLimit(request)
        │
        ├─ Step 1: clientRepository.findByApiKey(apiKey)
        │       └─ NOT FOUND → throw InvalidApiKeyException (401)
        │
        ├─ Step 2: client.getStatus() == INACTIVE?
        │       └─ YES → throw InvalidApiKeyException (401)
        │
        ├─ Step 3: bucketStateRepository.findByClient(client)
        │       └─ Uses @Lock(PESSIMISTIC_WRITE)
        │          Issues: SELECT ... FOR UPDATE
        │       └─ NOT FOUND → throw BucketNotFoundException (404)
        │
        ├─ Step 4: elapsed = now - bucket.lastRefillTime
        │
        ├─ Step 5: refillAmount = elapsed.seconds × requestsPerSecond
        │          refilledTokens = min(current + refill, burstCapacity)
        │
        ├─ Step 6a (tokens > 0):
        │       availableTokens -= 1
        │       allowed = true, retryAfter = 0
        │
        ├─ Step 6b (tokens == 0):
        │       allowed = false
        │       retryAfter = 1000 / requestsPerSecond  (ms)
        │
        ├─ Step 7: bucketStateRepository.save(bucket)  ← ALWAYS
        │
        └─ RateLimitResponse
                ├─ allowed=true  → 200 OK
                └─ allowed=false → 429 Too Many Requests
```

---

## 5. Token Bucket Algorithm Flow

```
                    Request arrives
                          │
                          ▼
              ┌───────────────────────┐
              │  Load BucketState     │
              │  (WITH LOCK)          │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  Compute elapsed time │
              │  since lastRefillTime │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  newTokens =          │
              │  elapsed_sec × RPS    │
              └───────────┬───────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  refilledTokens =     │
              │  min(current+new,     │
              │      burstCapacity)   │
              └───────────┬───────────┘
                          │
               ┌──────────▼──────────┐
               │  tokens > 0?         │
               └───┬─────────────┬───┘
                   │ YES         │ NO
                   ▼             ▼
         ┌─────────────┐  ┌─────────────┐
         │ tokens -= 1 │  │ allowed=    │
         │ allowed=true│  │   false     │
         │ retry=0     │  │ retry=      │
         └──────┬──────┘  │ 1000/RPS   │
                │         └──────┬──────┘
                └────────┬───────┘
                         │
                         ▼
              ┌───────────────────────┐
              │  Save BucketState     │
              │  (always, both paths) │
              └───────────┬───────────┘
                          │
                          ▼
                    Return Response
```

**Key Properties:**
- **Refill rate:** `requestsPerSecond` tokens per second, applied on demand (lazy evaluation — no background thread needed)
- **Burst capacity:** Maximum tokens the bucket can hold at any time
- **Concurrency safety:** Pessimistic `SELECT FOR UPDATE` ensures only one thread can evaluate and update the bucket at a time

---

## 6. Rate Limiting Sequence Diagram

```
Client App          RateLimiterController    RateLimiterServiceImpl    BucketStateRepository    MySQL
    │                       │                        │                         │                  │
    │─ POST /rate-limit ────►│                        │                         │                  │
    │                       │─ checkRateLimit(req) ──►│                         │                  │
    │                       │                        │── findByApiKey() ───────────────────────────►│
    │                       │                        │◄── Client ─────────────────────────────────-│
    │                       │                        │── findByClient() ───────►│                  │
    │                       │                        │                         │── SELECT FOR UPDATE►
    │                       │                        │                         │◄── BucketState ───│
    │                       │                        │◄── BucketState ─────────│                  │
    │                       │                        │                         │                  │
    │                       │   [Compute refill, consume token or deny]         │                  │
    │                       │                        │                         │                  │
    │                       │                        │── save(bucket) ─────────►│                  │
    │                       │                        │                         │── UPDATE ─────────►│
    │                       │                        │                         │◄── OK ────────────│
    │                       │◄── RateLimitResponse ──│                         │                  │
    │◄─ 200 or 429 ─────────│                        │                         │                  │
```

---

## 7. Layer-by-Layer Explanation

### Controller Layer
**Responsibility:** HTTP boundary only.

- Accept and validate request (delegated to `@Valid` + Jakarta Validation)
- Call the service
- Map the result to the appropriate `ResponseEntity` with the correct HTTP status
- Never contains business logic

### Service Layer
**Responsibility:** All business rules and algorithm implementation.

- `ClientServiceImpl`: CRUD orchestration, duplicate detection, bucket initialisation
- `RateLimiterServiceImpl`: Token Bucket algorithm — refill computation, token consumption, persistence
- Annotated with `@Transactional` to guarantee atomicity
- Throws domain exceptions (`ClientNotFoundException`, etc.)

### Repository Layer
**Responsibility:** Data access only.

- `ClientRepository`: Standard CRUD + `findByClientId`, `findByApiKey`, `existsByClientName`
- `BucketStateRepository`: Adds `@Lock(PESSIMISTIC_WRITE)` to `findByClient` — the critical section that prevents concurrent token over-consumption

### Mapper Layer
**Responsibility:** Object graph transformation.

- `ClientMapper`: Static utility converting `CreateClientRequest → Client` and `Client → ClientResponse`
- Generates `clientId` and `apiKey` on mapping — keeps IDs out of the request DTO

### Exception Handling
**Responsibility:** Consistent error contract.

- `GlobalExceptionHandler` catches all domain exceptions and returns structured JSON
- Uses `buildErrorResponse()` helper to avoid repetition
- Logs at `WARN` for expected errors, `ERROR` for data integrity failures

### Entity Layer
**Responsibility:** Database schema definition and lifecycle management.

- `@PrePersist` / `@PreUpdate` hooks maintain `createdAt` / `updatedAt` automatically
- `@Version` on `Client` prevents lost updates under concurrent `PUT` requests
- `@OneToOne(cascade = ALL, orphanRemoval = true)` ensures `BucketState` is never orphaned
