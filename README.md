# 🚗 Luxury Cars E-Commerce Backend

[![CI - Build & Test](https://github.com/AnkitByteWorks/Project---Luxury-Carstore-/actions/workflows/ci.yml/badge.svg)](https://github.com/AnkitByteWorks/Project---Luxury-Carstore-/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Docker](https://img.shields.io/badge/Docker-ready-blue)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-black)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green)
![Redis](https://img.shields.io/badge/Redis-7-red)

A production-grade, enterprise polyglot microservices-ready REST API for purchasing bespoke luxury hypercars and supercars. Built with **Spring Boot 4.1.1**, **Java 25**, **MySQL 8**, **MongoDB 7**, **Redis 7**, **Apache Kafka (KRaft mode)**, **JWT Authentication with Role-Based Access Control (RBAC)**, and **Docker**.

---

## ✨ System Architecture & Features

- 🔐 **JWT Security & RBAC**: Stateless RFC 7519 HMAC-SHA256 authentication with dual-tier roles (`ROLE_USER`, `ROLE_ADMIN`, and `ROLE_VIP` Secret Vault sessions).
- 🚗 **Bespoke Vehicle Catalog**: 20 luxury and hypercars with high-res multi-angle photography, technical specifications, and inventory tracking.
- 🏁 **Live Hypercar Auctions**: Real-time Server-Sent Events (SSE) bid streams, reserve pricing logic, and concurrent bid management.
- 🔒 **Classified Secret Vault**: Passcode-gated 1-of-1 hypercar allocations and VIP allocation dossiers with automatic token issuance.
- 💳 **Payment Gateway Integration**: Multi-channel payments (UPI QR code generation, Credit/Debit cards, Crypto Escrow) with secure webhook signature processing.
- 🛰️ **Polyglot Persistence**:
  - **MySQL 8 (Hibernate / Flyway)**: ACID transactions for cars, user profiles, orders, and auctions.
  - **MongoDB 7**: Rich JSON documents for unstructured 3D showroom clickstreams, parts inspection telemetry, and carrier IoT logs.
  - **Redis 7**: Distributed rate limiting (Bucket4j), token revocation cache, and real-time SSE channel multiplexing.
- ⚡ **Apache Kafka (KRaft Mode)**: High-throughput asynchronous event publishing for decoupled order placement and dealer dispatch pipelines.
- 📦 **Automated PDF Invoicing & Notifications**: iText-generated downloadable invoices and instant multi-channel alerts (Email & Telegram bot).
- 🧪 **Comprehensive Test Coverage**: 99 unit, controller (MockMvc), repository, and full-flow integration tests running on JUnit 5 and TestRestTemplate.

---

## ⚙️ Environment Variables Reference

Configure these in `.env` (copy from `.env.example`) or pass them directly to the container/shell:

| Variable | Required? | Default | Description |
|---|:---:|---|---|
| `PORT` | Optional | `8080` | Spring Boot server port |
| `SPRING_PROFILES_ACTIVE` | Optional | `dev` | Active Spring profile (`dev` or `prod`) |
| `DB_URL` | Required in prod | `jdbc:mysql://localhost:3306/luxury_cars_db...` | JDBC URL for MySQL database |
| `DB_USERNAME` | Required in prod | `root` | MySQL database username |
| `DB_PASSWORD` | Required in prod | `(empty)` | MySQL database password |
| `JWT_SECRET` | **Required in prod** | `(must be set via env)` | Secret key for HMAC-SHA256 JWT signing (min 32 bytes) |
| `JWT_EXPIRATION` | Optional | `86400000` | JWT token validity in milliseconds (24 hours) |
| `ADMIN_DEFAULT_PASSWORD` | Optional | `(auto-generated in prod)` | Initial password for `admin` user on first startup |
| `REDIS_HOST` | Optional | `localhost` | Redis server hostname |
| `REDIS_PORT` | Optional | `6379` | Redis server port |
| `SPRING_DATA_MONGODB_URI`| Optional | `mongodb://localhost:27017/carstore_audit` | MongoDB connection URI |
| `KAFKA_BOOTSTRAP_SERVERS`| Optional | `localhost:9092` | Apache Kafka broker address |
| `TELEGRAM_ENABLED` | Optional | `false` | Enable instant Telegram dealer notifications |
| `TELEGRAM_BOT_TOKEN` | Optional | `(empty)` | Telegram Bot API Token |
| `TELEGRAM_CHAT_ID` | Optional | `(empty)` | Telegram Chat ID for alerts |

---

## 🚀 Local Development & Execution

### Option 1: Docker Compose (All-in-One Infrastructure)

Runs MySQL 8, Redis 7, MongoDB 7, Kafka (KRaft), Mongo Express, Kafka UI, and the Spring Boot application:

```bash
# 1. Clone the repository
git clone https://github.com/AnkitByteWorks/Project---Luxury-Carstore-.git
cd Project---Luxury-Carstore-

# 2. Configure environment variables
cp .env.example .env
# Edit .env and supply your secure passwords & JWT_SECRET

# 3. Launch all containers
docker-compose up -d

# 4. Verify running services
docker-compose ps
```

Services exposed:
- **Backend API**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Kafka UI**: `http://localhost:8085`
- **Mongo Express UI**: `http://localhost:8081`

---

### Option 2: Local Maven Execution

Ensure you have **Java 25** and a local MySQL instance running:

```bash
# 1. Clone repository
git clone https://github.com/AnkitByteWorks/Project---Luxury-Carstore-.git
cd Project---Luxury-Carstore-

# 2. Export environment variables (PowerShell example)
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:JWT_SECRET="YourSecureJwtSigningSecretMustBeAtLeast32BytesLong!"
$env:ADMIN_DEFAULT_PASSWORD="YourSecureAdminPassword"

# 3. Run all unit and integration tests
mvn test

# 4. Start the application
mvn spring-boot:run
```

---

## 🔗 Connecting the Frontend

The companion Next.js frontend repository is located at [`Carstore-frontend-`](https://github.com/AnkitByteWorks/Carstore-frontend-.git).

### Running Frontend Locally

```bash
# 1. Clone frontend repository
git clone https://github.com/AnkitByteWorks/Carstore-frontend-.git
cd Carstore-frontend-

# 2. Install dependencies
npm install

# 3. Configure backend API connectivity
# In .env.local:
NEXT_PUBLIC_API_URL=http://localhost:8080

# 4. Start Next.js development server
npm run dev
```

Visit `http://localhost:3000` to interact with the full stack application.

---

## 🔐 Administrative Account

On initial database initialization:
- An administrative user (`username: admin`) is provisioned automatically.
- In **development**, the default password is configurable via `ADMIN_DEFAULT_PASSWORD`.
- In **production**, if `ADMIN_DEFAULT_PASSWORD` is omitted, the application securely generates a random one-time cryptographic secret on bootstrap and avoids printing plain-text credentials to logs.

---

## 🧪 Testing Suite

Run the full automated test suite containing 99 tests:

```bash
mvn clean test
```
