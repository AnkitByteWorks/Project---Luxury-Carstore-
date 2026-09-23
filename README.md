# 🚗 Luxury Cars E-Commerce Backend

[![CI - Build & Test](https://github.com/<your-username>/carstore/actions/workflows/ci.yml/badge.svg)](https://github.com/<your-username>/carstore/actions/workflows/ci.yml)

A production-grade REST API for buying luxury cars. Built with **Spring Boot 4**, **Java 25**, **MySQL**, **JWT auth**, and **Docker**.

## ✨ Features

- 🔐 JWT authentication + role-based access (ADMIN/USER)
- 🚗 20 luxury cars with images stored on disk
- 🛒 Order placement with customer info
- 🔍 Search, filter, pagination
- 📖 Swagger/OpenAPI documentation
- 🗄️ Flyway database migrations
- ⚡ Caffeine caching
- 🚦 Rate limiting (Bucket4j)
- 📝 Structured logging (SLF4J + Logback)
- 🧪 Comprehensive tests (JUnit 5, Mockito, MockMvc)
- 🐳 Dockerized (multi-stage build)

## 🚀 Quick Start

```bash
# Clone
git clone https://github.com/<AnkitByteWorks>/carstore.git
cd carstore

# Start everything
docker-compose up -d

# Access
open http://localhost:8080/swagger-ui.html
