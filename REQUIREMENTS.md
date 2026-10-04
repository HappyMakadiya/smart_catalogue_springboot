# SmartCatalog — Requirements & Setup Guide

> A Spring Boot–based smart product catalog with AI-powered semantic search, JWT auth, Redis rate-limiting, and PostgreSQL + pgvector storage.

---

## 📋 Table of Contents

- [Prerequisites](#-prerequisites)
- [Tech Stack](#-tech-stack)
- [Libraries & Dependencies](#-libraries--dependencies)
- [Environment Variables](#-environment-variables)
- [Database Configuration](#-database-configuration)
- [Step-by-Step: Running the Project](#-step-by-step-running-the-project)
- [Useful URLs](#-useful-urls)
- [Common Commands Reference](#-common-commands-reference)

---

## ✅ Prerequisites

Make sure the following tools are installed on your machine before starting:

| Tool | Required Version | Check Command |
|------|-----------------|---------------|
| **Java (JDK)** | 21 or higher | `java -version` |
| **Maven** | 3.9.x (bundled via `mvnw`) | `./mvnw -version` |
| **Docker** | Latest stable | `docker -version` |
| **Docker Compose** | v2.x (included with Docker Desktop) | `docker compose version` |
| **Git** | Any recent version | `git --version` |

> **Note:** Maven is bundled via the Maven Wrapper (`mvnw`). You do **not** need to install Maven globally — just use `./mvnw` instead of `mvn`.

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 4.1.1 |
| Build Tool | Apache Maven | 3.9.16 (via wrapper) |
| Database | PostgreSQL + pgvector extension | 16 (Docker image) |
| Cache / Rate-Limit Store | Redis | 7.2 (Docker image) |
| AI Embeddings | Google Gemini (`gemini-embedding-001`) | Spring AI 2.0.1 |
| DB Migrations | Flyway | (managed by Spring Boot) |
| ORM | Spring Data JPA / Hibernate | (managed by Spring Boot) |
| Security | Spring Security + JWT | JJWT 0.12.6 |
| Bean Mapping | MapStruct | 1.6.3 |
| CSV Parsing | OpenCSV | 5.12.0 |
| DB Admin UI | Adminer | Latest (Docker image) |

---

## 📦 Libraries & Dependencies

### Core Spring Boot Starters

| Dependency | Purpose |
|-----------|---------|
| `spring-boot-starter-webmvc` | REST API / web layer |
| `spring-boot-starter-data-jpa` | JPA/Hibernate ORM |
| `spring-boot-starter-validation` | Bean validation (`@Valid`, `@NotBlank`, etc.) |
| `spring-boot-starter-security` | Authentication & authorization |
| `spring-boot-starter-data-redis` | Redis client (Lettuce) |
| `spring-boot-starter-flyway` | Database schema migration |
| `spring-boot-devtools` | Hot reload during development |

### Third-Party Libraries

| Library | Group ID | Version | Purpose |
|---------|---------|---------|---------|
| PostgreSQL JDBC Driver | `org.postgresql` | (managed) | JDBC driver for PostgreSQL |
| Flyway PostgreSQL | `org.flywaydb` | (managed) | PostgreSQL-specific Flyway support |
| Lombok | `org.projectlombok` | (managed) | Boilerplate reduction (`@Getter`, `@Builder`, etc.) |
| MapStruct | `org.mapstruct` | 1.6.3 | Compile-time DTO ↔ Entity mapping |
| JJWT API | `io.jsonwebtoken:jjwt-api` | 0.12.6 | JWT creation & parsing |
| JJWT Impl | `io.jsonwebtoken:jjwt-impl` | 0.12.6 | JWT runtime implementation |
| JJWT Jackson | `io.jsonwebtoken:jjwt-jackson` | 0.12.6 | Jackson serialization for JWT |
| OpenCSV | `com.opencsv` | 5.12.0 | CSV file parsing for bulk product import |
| Bucket4j Core | `com.bucket4j:bucket4j-core` | 8.10.1 | Token-bucket rate limiting |
| Bucket4j Redis | `com.bucket4j:bucket4j-redis` | 8.10.1 | Distributed rate-limit state via Redis |
| Spring AI Google GenAI Embedding | `org.springframework.ai` | 2.0.1 | Gemini embedding model integration |

---

## 🔐 Environment Variables

Create a `.env` file at the project root (already present in the repo) with the following keys:

```env
# Google Gemini API key — get one free at https://aistudio.google.com/app/apikey
GEMINI_API_KEY=your_gemini_api_key_here
```

The app reads this variable via `application.properties`:
```properties
spring.ai.google.genai.embedding.api-key=${GEMINI_API_KEY:MISSING}
```

> ⚠️ **Never commit a real API key to version control.** The `.env` file should be listed in `.gitignore`.

---

## 🗄️ Database Configuration

The `application.properties` is pre-configured to connect to the Docker-managed PostgreSQL instance:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/spring_db
spring.datasource.username=db_user
spring.datasource.password=db_password
```

Redis connection (default, no auth):
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

JWT settings:
```properties
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000   # 24 hours in milliseconds
```

Flyway will **automatically run all migrations** in `src/main/resources/db/migration/` on startup.

---

## 🚀 Step-by-Step: Running the Project

### Step 1 — Clone the Repository

```bash
git clone <your-repo-url>
cd smartcatalog
```

### Step 2 — Set Up the Environment File

Open `.env` at the project root and fill in your **Gemini API key**:

```env
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

Get a free key at: https://aistudio.google.com/app/apikey

### Step 3 — Start Infrastructure Services (PostgreSQL + Redis + Adminer)

```bash
docker compose up -d
```

Wait for health checks to pass (about 10–15 seconds), then verify:

```bash
docker compose ps
# All services should show "healthy" or "running"
```

### Step 4 — Build the Application

```bash
./mvnw clean package -DskipTests
```

This compiles the code, runs annotation processors (Lombok + MapStruct), and packages the app as a JAR in `target/`.

### Step 5 — Run the Application

**Option A — Via Maven (recommended for development):**

```bash
./mvnw spring-boot:run
```

**Option B — Via the packaged JAR:**

```bash
java -jar target/smartcatalog-0.0.1-SNAPSHOT.jar
```

> On startup, Flyway will automatically apply any pending database migrations.

### Step 6 — Verify the Application is Running

```bash
curl http://localhost:8080/actuator/health
# or visit http://localhost:8080 in your browser
```

---

## 🌐 Useful URLs

| Service | URL | Notes |
|---------|-----|-------|
| Spring Boot App | http://localhost:8080 | Main REST API |
| Adminer (DB UI) | http://localhost:8081 | Web-based PostgreSQL client |
| PostgreSQL | `localhost:5432` | DB: `spring_db`, User: `db_user` |
| Redis | `localhost:6379` | No auth by default |

**Adminer login details:**
- System: `PostgreSQL`
- Server: `postgres`
- Username: `db_user`
- Password: `db_password`
- Database: `spring_db`

---

## 📌 Common Commands Reference

```bash
# ── Docker ──────────────────────────────────────────────
docker compose up -d              # Start all services in background
docker compose down               # Stop and remove containers
docker compose down -v            # Stop containers AND wipe volumes (fresh DB)
docker compose ps                 # Check service status
docker compose logs -f postgres   # Follow PostgreSQL logs
docker compose logs -f redis      # Follow Redis logs

# ── Maven / Build ───────────────────────────────────────
./mvnw clean install              # Full build + run all tests
./mvnw clean package -DskipTests  # Build JAR, skip tests
./mvnw spring-boot:run            # Run app with hot-reload (devtools)
./mvnw test                       # Run all tests only
./mvnw dependency:tree            # Show full dependency tree

# ── Running the JAR ─────────────────────────────────────
java -jar target/smartcatalog-0.0.1-SNAPSHOT.jar

# ── Git ─────────────────────────────────────────────────
git status
git pull origin main

# ── Quick Health Check ──────────────────────────────────
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

---

## 🧹 Stopping & Cleanup

```bash
# Stop the app: press Ctrl+C in the terminal running Spring Boot

# Stop Docker services (preserve data):
docker compose down

# Stop Docker services AND wipe ALL data (fresh start):
docker compose down -v
```

---

*Last updated: October 2026*
