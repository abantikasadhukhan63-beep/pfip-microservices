# pfip-microservices

> A production-ready, secure REST API backend built with Spring Boot 3 and Java 21, following a microservices architecture. Handles user authentication with JWT tokens and financial transaction management — all running behind a unified API Gateway.

---

## What This Project Does

This is a backend system for a **Personal Finance & Investment Platform (pfip)**. It allows users to register, log in securely, and manage their financial transactions. The system is split into independent services that communicate through an API Gateway, making it scalable, maintainable, and easy to extend.

A user can register an account, receive a JWT token, and use that token to create and query their transactions — credits, debits, balances, and summaries — all without any service knowing more than it needs to.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Gateway | Spring Cloud Gateway (WebFlux / reactive) |
| Database | PostgreSQL 16 (two separate instances) |
| ORM | Spring Data JPA + Hibernate 6 |
| Containerisation | Docker + Docker Compose |
| Build Tool | Maven 3.9 (multi-module) |
| Validation | Jakarta Validation |
| Boilerplate | Lombok |

---

## Architecture

```
Client (Postman / Browser / App)
              │
              ▼  :8080
      ┌───────────────┐
      │  API Gateway  │  ← Single public entry point
      │               │    Validates JWT on every protected request
      │               │    Injects X-Username + X-User-Id headers
      └───────┬───────┘
              │
     ┌────────┴────────┐
     ▼                 ▼
:8081              :8082
┌──────────────┐   ┌──────────────────────┐
│ User Service │   │ Transaction Service  │
│              │   │                      │
│ Register     │   │ Create transaction   │
│ Login        │   │ List transactions    │
│ JWT issuance │   │ Filter by type/date  │
│ User profile │   │ Balance summary      │
└──────┬───────┘   └──────────┬───────────┘
       │                      │
       ▼                      ▼
  PostgreSQL              PostgreSQL
  pfip_users          pfip_transactions
  port 5432               port 5433
```

Each service has its own database. They share nothing at the data layer — the only thing they share is the JWT secret key used for token signing and verification.

---

## Project Structure

```
pfip-microservices/
├── pom.xml                              ← Parent POM — manages all versions
├── docker-compose.yml                   ← Full stack: 2 DBs + 3 services
├── README.md
│
├── common/                              ← Shared library (not executable)
│   └── src/main/java/com/pfip/common/
│       ├── dto/
│       │   ├── ApiResponse.java         ← Generic { success, data, error } wrapper
│       │   └── ErrorResponse.java       ← Validation error envelope
│       └── exception/
│           ├── ResourceNotFoundException.java
│           └── DuplicateResourceException.java
│
├── user-service/                        ← Runs on port 8081
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/pfip/userservice/
│       ├── UserServiceApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java      ← CSRF off, stateless, JWT filter
│       │   └── GlobalExceptionHandler.java
│       ├── controller/
│       │   ├── AuthController.java      ← POST /api/auth/register, /login
│       │   └── UserController.java      ← GET /api/users/me, /{id}
│       ├── dto/
│       │   └── AuthDtos.java            ← RegisterRequest, LoginRequest, AuthResponse
│       ├── entity/
│       │   └── User.java                ← JPA entity → users table
│       ├── filter/
│       │   └── JwtAuthenticationFilter.java
│       ├── repository/
│       │   └── UserRepository.java
│       ├── security/
│       │   ├── JwtUtil.java             ← Generate + validate tokens
│       │   └── UserDetailsServiceImpl.java
│       └── service/
│           ├── AuthService.java         ← Register, login, JWT issuance
│           └── UserService.java         ← Profile management
│
├── transaction-service/                 ← Runs on port 8082
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/pfip/transactionservice/
│       ├── TransactionServiceApplication.java
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   └── GlobalExceptionHandler.java
│       ├── controller/
│       │   └── TransactionController.java  ← 7 REST endpoints
│       ├── dto/
│       │   └── TransactionDtos.java
│       ├── entity/
│       │   └── Transaction.java         ← JPA entity → transactions table
│       ├── filter/
│       │   └── JwtAuthenticationFilter.java
│       ├── repository/
│       │   └── TransactionRepository.java
│       ├── security/
│       │   └── JwtUtil.java             ← Validate only (no generation)
│       └── service/
│           └── TransactionService.java
│
└── api-gateway/                         ← Runs on port 8080
    ├── pom.xml
    ├── Dockerfile
    └── src/main/java/com/pfip/gateway/
        ├── ApiGatewayApplication.java
        ├── config/
        │   └── CorsConfig.java          ← Global CORS policy
        └── filter/
            ├── JwtAuthenticationFilter.java  ← GlobalFilter — runs on every request
            └── JwtUtil.java
```

---

## Prerequisites

Make sure you have all of these installed before starting:

| Tool | Version | Purpose |
|---|---|---|
| JDK | 21 | Compile and run Java |
| Maven | 3.9+ | Build tool |
| Docker Desktop | Latest | Run containers |
| Git | Any | Version control |
| IntelliJ IDEA | Community or Ultimate | IDE |

---

## Getting Started

### Step 1 — Clone the repository

```bash
git clone https://github.com/your-username/pfip-microservices.git
cd pfip-microservices
```

### Step 2 — Build all JARs

Run this from the project root. This compiles all four modules and packages them into executable JARs:

```bash
mvn clean package -DskipTests
```

Wait for `BUILD SUCCESS` before proceeding.

### Step 3 — Start the full stack

```bash
docker-compose up --build
```

This starts everything in the correct order:
1. PostgreSQL databases (users + transactions)
2. user-service (waits for its DB to be healthy)
3. transaction-service (waits for its DB to be healthy)
4. api-gateway (waits for both services to be healthy)

### Step 4 — Verify everything is running

Open a second terminal and run:

```bash
docker-compose ps
```

All five containers should show `healthy`:

```
pfip-postgres-users         running (healthy)
pfip-postgres-transactions  running (healthy)
pfip-user-service           running (healthy)
pfip-transaction-service    running (healthy)
pfip-api-gateway            running (healthy)
```

---

## API Reference

All requests go through the gateway at `http://localhost:8080`. You never call port 8081 or 8082 directly.

### Authentication — no token required

#### Register a new user
```
POST /api/auth/register
```
Request body:
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123",
  "firstName": "Alice",
  "lastName": "Smith"
}
```
Response `201 Created`:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 1,
      "username": "alice",
      "email": "alice@example.com",
      "role": "USER"
    }
  }
}
```

#### Login
```
POST /api/auth/login
```
Request body:
```json
{
  "username": "alice",
  "password": "password123"
}
```
Response `200 OK` — same structure as register, includes a fresh JWT token.

---

### Users — token required

Add `Authorization: Bearer <your_token>` header to all requests below.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/users/me` | Get your own profile | Any authenticated user |
| GET | `/api/users/{id}` | Get user by ID | Any authenticated user |
| GET | `/api/users` | List all users | ADMIN only |
| PATCH | `/api/users/{id}/toggle` | Enable/disable user | ADMIN only |

---

### Transactions — token required

Add `Authorization: Bearer <your_token>` header to all requests below.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/transactions` | Create a transaction |
| GET | `/api/transactions` | List your transactions (paged) |
| GET | `/api/transactions/{id}` | Get a single transaction |
| GET | `/api/transactions/type/{type}` | Filter by CREDIT or DEBIT |
| GET | `/api/transactions/range` | Filter by date range |
| GET | `/api/transactions/summary` | Get balance and totals |
| PATCH | `/api/transactions/{id}/status` | Update transaction status |

#### Create a transaction — example request body
```json
{
  "amount": 5000.00,
  "type": "CREDIT",
  "currency": "USD",
  "description": "Monthly salary",
  "category": "SALARY"
}
```

#### Filter by date range
```
GET /api/transactions/range?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
```

#### Summary response example
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "totalCredits": 10000.00,
    "totalDebits": 3500.00,
    "netBalance": 6500.00,
    "transactionCount": 8
  }
}
```

#### Pagination parameters (on list endpoints)
| Parameter | Default | Description |
|---|---|---|
| `page` | 0 | Page number (zero-based) |
| `size` | 20 | Items per page |
| `sortBy` | createdAt | Field to sort by |
| `direction` | desc | `asc` or `desc` |

---

## Testing with curl (Windows)

Use `curl.exe` in PowerShell or `curl` in Git Bash.

#### Register
```bash
curl.exe -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"password123\",\"firstName\":\"Alice\",\"lastName\":\"Smith\"}"
```

#### Login and save token
```bash
curl.exe -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"alice\",\"password\":\"password123\"}"
```
Copy the `accessToken` value from the response.

#### Get your profile
```bash
curl.exe -X GET http://localhost:8080/api/users/me -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

#### Create a transaction
```bash
curl.exe -X POST http://localhost:8080/api/transactions -H "Content-Type: application/json" -H "Authorization: Bearer YOUR_TOKEN_HERE" -d "{\"amount\":500.00,\"type\":\"CREDIT\",\"currency\":\"USD\",\"description\":\"Salary\",\"category\":\"SALARY\"}"
```

#### Get balance summary
```bash
curl.exe -X GET http://localhost:8080/api/transactions/summary -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## Configuration

### Environment variables

All configuration is driven by environment variables injected by Docker Compose. The `application.yml` files use `${VAR_NAME:default}` syntax — the value after the colon is the fallback used when running locally without Docker.

| Variable | Used by | Description |
|---|---|---|
| `DB_HOST` | user-service, transaction-service | PostgreSQL hostname |
| `DB_PORT` | user-service, transaction-service | PostgreSQL port |
| `DB_NAME` | user-service, transaction-service | Database name |
| `DB_USER` | user-service, transaction-service | Database username |
| `DB_PASS` | user-service, transaction-service | Database password |
| `JWT_SECRET` | all three services | Base64-encoded HMAC-SHA256 signing key |
| `JWT_EXPIRATION` | user-service | Token lifetime in milliseconds (default 86400000 = 24h) |
| `USER_SERVICE_URL` | api-gateway | URL of user-service inside Docker network |
| `TRANSACTION_SERVICE_URL` | api-gateway | URL of transaction-service inside Docker network |

### JWT secret

The default secret is a development placeholder. For production, generate a strong one:

```bash
# Mac / Linux
openssl rand -base64 64

# Windows PowerShell
[Convert]::ToBase64String((1..64 | ForEach-Object { [byte](Get-Random -Max 256) }))
```

Set the same generated value for `JWT_SECRET` in all three services in `docker-compose.yml`. If even one character differs between services, every API call will return 401.

### Ports

| Service | Host port | Container port |
|---|---|---|
| api-gateway | 8080 | 8080 |
| user-service | 8081 | 8081 |
| transaction-service | 8082 | 8082 |
| postgres-users | 5432 | 5432 |
| postgres-transactions | 5433 | 5432 |

---

## Common Commands

### Start everything
```bash
docker-compose up --build
```

### Stop everything (keep database data)
```bash
docker-compose down
```

### Stop everything and wipe database data
```bash
docker-compose down --volumes
```

### View logs for a specific service
```bash
docker-compose logs user-service
docker-compose logs transaction-service
docker-compose logs api-gateway
```

### Rebuild after code changes
```bash
mvn clean package -DskipTests
docker-compose down
docker-compose up --build
```

### Run locally without Docker (databases must be running separately)
```bash
# Start only the databases
docker-compose up postgres-users postgres-transactions

# Run services from IntelliJ by clicking the green play button next to:
# UserServiceApplication, TransactionServiceApplication, ApiGatewayApplication
# Start them in that order
```

---

## How Security Works

1. User calls `POST /api/auth/login` with username and password.
2. user-service verifies credentials against the database using BCrypt password comparison.
3. On success, user-service generates a JWT token signed with the shared HMAC-SHA256 secret and returns it.
4. For every subsequent request, the client includes the token in the `Authorization: Bearer <token>` header.
5. The API Gateway's `JwtAuthenticationFilter` intercepts the request, validates the token signature and expiry, and extracts the username.
6. The gateway injects `X-Username` and `X-User-Id` headers into the forwarded request.
7. Downstream services (user-service, transaction-service) read the `X-User-Id` header to identify the caller — they never re-query the user database.
8. Each service also validates the JWT independently as a second layer of security.

Public endpoints (`/api/auth/register`, `/api/auth/login`) bypass the JWT check entirely.

---

## Error Responses

All errors follow the same structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: '99'",
  "path": "/api/users/99",
  "timestamp": "2024-01-15T10:30:00"
}
```

| HTTP Status | When it happens |
|---|---|
| 400 Bad Request | Validation failed (missing field, invalid email, etc.) |
| 401 Unauthorized | Missing, expired, or invalid JWT token |
| 403 Forbidden | Valid token but insufficient role (e.g. non-admin accessing admin endpoint) |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Username or email already registered |
| 500 Internal Server Error | Unexpected server error |

---

## Transaction Types and Statuses

**Types:**
- `CREDIT` — money coming in (salary, transfer received, refund)
- `DEBIT` — money going out (purchase, bill, withdrawal)

**Statuses:**
- `PENDING` — created but not yet processed
- `COMPLETED` — successfully processed
- `FAILED` — processing failed
- `REVERSED` — transaction was reversed/refunded

---

## Known Limitations and Next Steps

- **User ID in JWT** — the gateway currently forwards `X-User-Id: 0` as a placeholder. To fix this, add a `userId` claim when generating the token in `AuthService` and extract it in the gateway filter.
- **No refresh tokens** — tokens expire after 24 hours and the user must log in again. Add a `RefreshToken` entity and `/api/auth/refresh` endpoint to support silent re-authentication.
- **No service discovery** — services are addressed by hardcoded Docker hostnames. For production, use Kubernetes service names or Spring Cloud Eureka.
- **No rate limiting** — the gateway does not limit request frequency. Add Spring Cloud Gateway's `RequestRateLimiter` filter backed by Redis.
- **No email verification** — users are activated immediately on registration without email confirmation.
- **Single currency** — the transaction summary does not aggregate across currencies.

---

## Troubleshooting

**`Failed to configure a DataSource`**
Your `application.yml` is not being found by Spring. Check that the file is at `src/main/resources/application.yml` (note: `resources` with an `s`). Also make sure the folder is marked as a Resources Root in IntelliJ (right-click → Mark Directory as → Resources Root).

**`password authentication failed for user "postgres"`**
Old Docker volumes contain stale credentials. Run `docker-compose down --volumes` to wipe them, then `docker-compose up --build`.

**`Unable to find GatewayFilterFactory`**
A filter name in `api-gateway/application.yml` does not match any registered filter class. The `JwtAuthenticationFilter` in this project is a `GlobalFilter` and does not need to be referenced in the yml at all — remove any filter references from the routes.

**`BUILD SUCCESS` but changes not reflected**
You edited a file but did not rebuild the jar. Always run `mvn clean package -DskipTests` before `docker-compose up --build`. The Dockerfile copies the pre-built jar from `target/` — it does not compile your code.

**Services start but API returns 404**
Check that you are calling port `8080` (the gateway) and not `8081` or `8082` directly. All requests must go through the gateway.

**`curl` not working in PowerShell**
Use `curl.exe` instead of `curl` in PowerShell. Or switch to Git Bash where standard curl works normally.
