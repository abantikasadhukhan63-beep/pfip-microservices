# Personal Finance & Investment Platform (PFIP) - Full Stack Architecture

> A production-ready, secure, and fully-containerised full-stack application built with Angular 17 and Spring Boot 3 microservices. It features JWT-based authentication, reactive API Gateway routing, and separate database instances per service for true microservice isolation.

---

## Overview

The **Personal Finance & Investment Platform (PFIP)** is a comprehensive full-stack solution designed to help users track their financial activities. The system leverages a robust Java Spring Boot microservices backend communicating via a reactive API Gateway, paired with a modern, dynamic Angular 17 frontend styled with Tailwind CSS.

By separating domains into independent microservices (Users, Transactions, Auth) with their own dedicated databases, the platform ensures high scalability and resilience. The frontend seamlessly interacts with these services through the gateway, offering a smooth user experience for authentication, profile management, and transaction tracking.

---

## Tech Stack

### Backend
| Technology | Version / Details | Purpose |
|---|---|---|
| Java | 21 | Core programming language |
| Spring Boot | 3.2.5 | Microservices framework |
| Spring Cloud Gateway | Reactive | API Routing and entry point |
| PostgreSQL | 16 | Relational data storage (per service) |
| Redis | 7 | Token caching and session management |
| JWT (jjwt) | 0.12.5 | Secure stateless authentication |
| Maven | 3.9 | Multi-module build management |
| Docker & Compose | Latest | Containerisation & Orchestration |

### Frontend
| Technology | Version / Details | Purpose |
|---|---|---|
| Angular | 17.3.0 | Component-based SPA framework |
| Tailwind CSS | 3.4.19 | Utility-first styling |
| RxJS | 7.8.0 | Reactive state and data flow |
| Three.js | 0.184.0 | 3D rendering and animations |
| Node.js | LTS | Build environment and package management |
| Nginx | Alpine | Static file serving in Docker |

---

## System Architecture

```text
                                  ┌──────────────────────────┐
                                  │      Client Browser      │
                                  │  (Angular 17 Frontend)   │
                                  └─────────────┬────────────┘
                                                │
                                        HTTP / REST API
                                                │
                                  ▼─────────────┴────────────▼
                                  │       API GATEWAY        │ :8080
                                  │   (Spring Cloud Gateway) │
                                  └──────┬────────────┬──────┘
                                         │            │
             ┌───────────────────────────┼────────────┼───────────────────────────┐
             │                           │            │                           │
             ▼ :8083                     ▼ :8081      ▼ :8082                     │
    ┌─────────────────┐       ┌─────────────────┐ ┌──────────────────────┐        │
    │  Auth Service   │───────▶  User Service   │ │ Transaction Service  │        │
    │ (Redis Cache)   │       │ (PostgreSQL)    │ │ (PostgreSQL)         │        │
    └─────────────────┘       └─────────────────┘ └──────────────────────┘        │
             │                           │                    │                   │
             ▼ :6379                     ▼ :5432              ▼ :5433             │
        ┌─────────┐                ┌───────────┐         ┌──────────────┐         │
        │  Redis  │                │ pfip_users│         │ pfip_trans...│         │
        └─────────┘                └───────────┘         └──────────────┘         │
```

---

## Project Structure

```text
pfip-microservices/
├── docker-compose.yml                   ← Full stack orchestrator (Backend + Frontend)
├── pom.xml                              ← Parent Maven POM
├── README.md                            ← Project documentation
│
├── frontend-service/                    ← Angular 17 Application (Runs on port 4200/80)
│   ├── src/app/
│   │   ├── core/                        ← Singleton services, guards, interceptors
│   │   ├── features/                    ← Feature modules (auth, dashboard, profile, transactions)
│   │   ├── layout/                      ← Main page layouts (header, sidebar, footer)
│   │   └── app.routes.ts                ← Frontend routing configuration
│   ├── Dockerfile                       ← Nginx-based frontend container build
│   ├── tailwind.config.js               ← Tailwind CSS design tokens
│   └── package.json                     ← NPM dependencies
│
├── api-gateway/                         ← Spring Cloud Gateway (Port 8080)
│   └── src/main/java/com/pfip/gateway/  ← Routes all incoming API requests
│
├── auth-service/                        ← Authentication microservice (Port 8083)
│   └── src/main/java/com/pfip/auth/     ← Handles login, JWT issuance, and validation
│
├── user-service/                        ← User management microservice (Port 8081)
│   └── src/main/java/com/pfip/user/     ← Manages profiles, talks to pfip_users DB
│
├── transaction-service/                 ← Transaction microservice (Port 8082)
│   └── src/main/java/com/pfip/trans/    ← CRUD for financial records, talks to pfip_transactions DB
│
└── common/                              ← Shared Java libraries
    └── src/main/java/com/pfip/common/   ← DTOs, Exceptions, and Utility classes
```

---

## Microservices Breakdown

### API Gateway (`api-gateway`)
- **Purpose**: Single entry point for all frontend requests. Routes traffic to appropriate microservices.
- **Tech**: Spring Cloud Gateway, WebFlux.
- **Role**: Injects CORS headers, routes API calls, but delegates JWT validation to the Auth Service.

### Auth Service (`auth-service`)
- **Purpose**: Centralised authentication provider.
- **Tech**: Spring Boot, Spring Security, Redis.
- **Role**: Issues JWTs upon successful login (validated against User Service) and verifies tokens on subsequent requests. Connects to Redis for session caching.

### User Service (`user-service`)
- **Purpose**: Manages user profiles and registration.
- **Tech**: Spring Boot, Spring Data JPA.
- **Database**: PostgreSQL (`pfip_users`).
- **Role**: Stores user credentials, handles profile updates, and serves user data to the Auth Service during login.

### Transaction Service (`transaction-service`)
- **Purpose**: Financial tracking and logging.
- **Tech**: Spring Boot, Spring Data JPA.
- **Database**: PostgreSQL (`pfip_transactions`).
- **Role**: Processes credits/debits, calculates balance summaries, and filters transaction histories.

### Shared Library (`common`)
- **Purpose**: Code reuse across backend services.
- **Role**: Centralises API response wrappers, global exception handling, and common DTOs.

---

## Frontend Architecture

The Angular frontend is strictly modularised:
- **Core Module**: Contains HTTP interceptors (for injecting JWTs), Route Guards (for protecting authenticated pages), and core API services.
- **Features**:
  - `auth`: Login and Registration forms.
  - `dashboard`: Financial summary and quick actions.
  - `transactions`: Data tables and forms for creating/editing transactions.
  - `profile`: User settings and data display.
- **Layout**: Shared UI components like Navigation bars and Footers.
- **State Management**: Uses RxJS `BehaviorSubject` to keep track of user authentication state dynamically.

---

## Authentication & Security

1. **Login Flow**: Frontend submits credentials to `API Gateway` -> `Auth Service` -> `User Service` validates -> `Auth Service` signs JWT -> Frontend receives token.
2. **Token Storage**: Frontend stores the JWT securely in `localStorage` or memory, depending on the environment setup.
3. **HTTP Interceptor**: The Angular `AuthInterceptor` attaches `Authorization: Bearer <token>` to all outbound backend requests automatically.
4. **Auth Guard**: Angular `AuthGuard` prevents unauthenticated users from accessing protected routes (e.g., Dashboard), redirecting them to the login page.
5. **Backend Verification**: API Gateway/Microservices use the shared `JWT_SECRET` to validate the token signature before processing any data.

---

## Prerequisites & Setup

Ensure you have the following installed:
- **Java 21**
- **Node.js** (v18+ LTS) and **npm**
- **Docker** and **Docker Compose**
- **Maven** (3.9+)

---

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/your-username/pfip-microservices.git
cd pfip-microservices
```

### 2. Build the Backend
Compile all Java modules:
```bash
mvn clean package -DskipTests
```

### 3. Install Frontend Dependencies (Optional for local dev)
```bash
cd frontend-service
npm install
cd ..
```

---

## Running with Docker

The easiest way to run the entire stack (Frontend, Gateway, Services, Redis, Databases) is via Docker Compose:

```bash
docker-compose up --build
```

**What happens?**
1. Postgres DBs and Redis start first.
2. Microservices boot up and connect to their databases.
3. API Gateway starts routing.
4. The Angular frontend is built and served via Nginx.

**Access the Application:**
- Frontend Web App: `http://localhost:4200`
- API Gateway: `http://localhost:8080`

Verify container health:
```bash
docker-compose ps
```

---

## Complete API Reference

Base URL: `http://localhost:8080`

### Authentication (Public)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user account |
| POST | `/api/auth/login` | Authenticate and receive JWT |

### Users (Protected)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users/me` | Fetch authenticated user's profile |
| GET | `/api/users/{id}` | Fetch specific user |

### Transactions (Protected)
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/transactions` | Create a new transaction |
| GET | `/api/transactions` | List all transactions (paginated) |
| GET | `/api/transactions/summary`| Get user balance and totals |
| GET | `/api/transactions/{id}` | Get specific transaction details |

*Note: All protected endpoints require the `Authorization: Bearer <jwt_token>` header.*

---

## Database Schema

### `pfip_users` (User Service)
- **Table:** `users`
  - `id` (PK, BigInt)
  - `username` (Varchar, Unique)
  - `password` (Varchar, Hashed)
  - `email` (Varchar, Unique)
  - `first_name`, `last_name`
  - `role` (Enum: USER, ADMIN)

### `pfip_transactions` (Transaction Service)
- **Table:** `transactions`
  - `id` (PK, BigInt)
  - `user_id` (BigInt, references user in User Service)
  - `amount` (Decimal)
  - `type` (Enum: CREDIT, DEBIT)
  - `status` (Enum: PENDING, COMPLETED, FAILED)
  - `description` (Varchar)
  - `created_at`, `updated_at` (Timestamps)

---

## Frontend Pages Documentation

1. **/login**: Captures user credentials and stores the resulting JWT.
2. **/register**: Form to create a new profile. Validates input fields.
3. **/dashboard**: Protected route. Displays a summary of the user's financial health by calling `/api/transactions/summary`.
4. **/transactions**: Protected route. Lists full history with filtering options. Allows creating new CREDIT/DEBIT entries.
5. **/profile**: Protected route. Displays user details fetched from `/api/users/me`.

---

## Configuration

Core environment variables handled via `docker-compose.yml`:
- `JWT_SECRET`: Must be identical across API Gateway, Auth Service, User Service, and Transaction Service.
- `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASS`: Database credentials.
- `REDIS_HOST`, `REDIS_PORT`: Caching configurations.

Frontend config (`environment.ts`):
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### Ports Overview
| Service | Internal | External |
|---|---|---|
| Frontend | 80 | 4200 |
| API Gateway | 8080 | 8080 |
| User Service | 8081 | 8081 |
| Transaction | 8082 | 8082 |
| Auth Service| 8083 | 8083 |
| Postgres DBs| 5432 | 5432 / 5433 |
| Redis | 6379 | 6379 |

---

## Common Commands

```bash
# Start full stack
docker-compose up -d

# Stop full stack
docker-compose down

# Wipe database volumes (Factory Reset)
docker-compose down --volumes

# Rebuild specific service
docker-compose up -d --build frontend
```

---

## Troubleshooting

- **CORS Errors**: Ensure the API Gateway is properly configured to allow origins from `http://localhost:4200`.
- **401 Unauthorized on all endpoints**: Verify that the `JWT_SECRET` in `docker-compose.yml` exactly matches across all services.
- **Frontend Container Failing to Start**: Check that Nginx is properly configured in the frontend `Dockerfile`.
- **Database Connection Refused**: Services might have started before Postgres was fully ready. The `healthcheck` in compose usually handles this, but a manual restart of the specific service might be needed.

---

## Known Limitations & Future Enhancements

- **Micro-frontend Architecture**: Break down the Angular app into smaller deployable chunks using Webpack Module Federation.
- **Event-Driven Comm**: Replace direct HTTP calls between microservices with Kafka or RabbitMQ.
- **Refresh Tokens**: Implement short-lived access tokens with secure refresh tokens to enhance security.
- **Full Test Coverage**: Expand unit and e2e testing for the Angular frontend using Cypress.

---

## Deployment & Production

For production environments:
1. Build the frontend for production using `ng build --configuration production`.
2. Secure the JWT Secret using a secrets manager.
3. Expose only the frontend container and API Gateway to the public network; keep all other microservices and databases in a private subnet.
4. Scale stateless microservices (User, Transaction, Gateway) using Kubernetes.

---

## Contributing & Code Standards

- **Code Style**: Ensure backend code adheres to standard Java conventions. Format frontend code using Prettier and TSLint.
- **Commits**: Use semantic commit messages (e.g., `feat: added transaction summary`, `fix: gateway CORS issue`).
- **PRs**: All PRs must pass CI build and test pipelines before being merged.
