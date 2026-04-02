# Gayakini Backend API

Professional e-commerce RESTful API built with Spring Boot 3.4+ and Kotlin 2.0, following Modular Monolith and Domain-Driven Design (DDD) principles.

## 🚀 Quick Start (Local Development)

This project is optimized for a native local development experience without requiring Docker by default, although Docker support is provided.

### 1. Prerequisites
- **JDK 17+**
- **PostgreSQL 18+** (installed on your OS or running in a container)

### 2. Environment Setup
Initialize your local environment variables:
```bash
./gradlew localSetup
```
Then, edit the generated `.env` file if your local PostgreSQL credentials differ from:
- `DB_USERNAME=postgres`
- `DB_PASSWORD=password`
- `DB_NAME=gayakini`

### 3. Run Preflight Check
Ensure your system is ready:
```bash
./gradlew doctor
```

### 4. Run Application
Start the server with the `local` profile:
```bash
./gradlew bootRunLocal
```

Once started, access the following:
- **API Base:** `http://localhost:8080/api/v1`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **Actuator Health:** `http://localhost:8080/actuator/health`

## 🛠 Developer Workflow & CLI

We use custom Gradle tasks to streamline development:

| Command | Description |
|---------|-------------|
| `./gradlew doctor` | Checks Java, DB connectivity, and `.env` status. |
| `./gradlew localSetup` | Initializes `.env` from template. |
| `./gradlew bootRunLocal` | Preflight check + Run with `local` profile. |
| `./gradlew qaAll` | Runs Lint (ktlint), Static Analysis (detekt), and Tests. |
| `./gradlew verifyMigrations` | Validates Flyway migration history. |
| `./gradlew releaseCheck` | Final quality gate before pushing (Lints + Tests + Migrations). |

## 📂 Project Structure & Architecture

The repository follows a domain-centric modular monolith structure. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

```text
src/main/kotlin/com/gayakini/
├── common/           # Cross-cutting concerns (UUIDv7, Idempotency)
├── infrastructure/   # Security, JWT, DB configurations
├── [domain]/         # Domain boundaries (Catalog, Order, Payment, etc.)
│   ├── api/          # Controllers & DTOs
│   ├── application/  # Services (Use Cases)
│   └── domain/       # Entities & Repositories
└── GayakiniApplication.kt
```

## 🧪 Testing & QA

### Automated Testing
- **Unit & Integration Tests:** `./gradlew test`
- **Quality Gate:** `./gradlew releaseCheck`

### Manual/HTTP Testing
We use `.http` files for a Postman-free experience. These are compatible with IntelliJ IDEA and VS Code (REST Client).
Located in `http/`:
- `smoke.http`: Health and basic connectivity.
- `auth.http`: User registration and JWT login.
- `order-flow.http`: End-to-end checkout to order conversion.
See [docs/TESTING.md](docs/TESTING.md) for more details.

## 📈 Monitoring & Observability
- **Logging:** Configured via `logback-spring.xml` with color-coded console output for development.
- **Metrics:** Micrometer integration with Prometheus endpoint at `/actuator/prometheus`.
- **Health Checks:** Detailed status at `/actuator/health`.

---
*Created by Principal Backend Engineering Team.*
