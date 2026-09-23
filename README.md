# 🚗 Luxury Cars E-Commerce Backend

[![CI - Build & Test](https://github.com/AnkitByteWorks/carstore/actions/workflows/ci.yml/badge.svg)](https://github.com/AnkitByteWorks/carstore/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8-blue)
![Docker](https://img.shields.io/badge/Docker-ready-blue)

A production-grade REST API for buying luxury cars. Built with **Spring Boot 4**, **Java 25**, **MySQL**, **JWT auth**, and **Docker**.

## ✨ Features

- 🔐 **JWT authentication** with role-based access (ADMIN/USER)
- 🚗 **20 luxury cars** with images stored on disk
- 🛒 **Order placement** with customer info
- 🔍 **Search, filter, pagination** on car listings
- 📖 **Swagger/OpenAPI** documentation
- 🗄️ **Flyway** database migrations
- ⚡ **Caffeine caching** for fast reads
- 🚦 **Rate limiting** (Bucket4j) per endpoint/IP
- 📝 **Structured logging** (SLF4J + Logback, rotating files)
- 🧪 **26 tests** (JUnit 5, Mockito, MockMvc, H2)
- 🐳 **Dockerized** with multi-stage builds
- ⚙️ **GitHub Actions CI** — automated test + Docker build

## 🚀 Quick Start

### Option 1: Docker (Recommended)

```bash
git clone https://github.com/AnkitByteWorks/carstore.git
cd carstore
docker-compose up -d

