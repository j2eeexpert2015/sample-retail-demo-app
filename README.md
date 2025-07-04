# sample-retail-demo-app

This Spring Boot application demonstrates the performance and functionality comparison between **Platform Threads (PT)** and **Virtual Threads (VT)** using a real-world use case with:
- PostgreSQL database interaction
- External REST API simulation using WireMock
- Testcontainers for containerized development setup

---

## 🔧 Tech Stack

- Java 21
- Spring Boot 3.1
- Testcontainers (PostgreSQL + WireMock)
- WireMock standalone in Docker
- HikariCP + Spring Data JPA
- Maven

---

## 📦 Project Modules

This is a single-module Spring Boot application with:

- **REST endpoints** for product fetch
- **PostgreSQL** integration using JPA
- **WireMock** container for simulating an external product service
- **ServiceConnection** and `TestConfiguration` to auto-start containers in dev

---

## 🐳 Testcontainers Configuration

Two containers run automatically when the app is launched:
- PostgreSQL (with DB name: `retaildb`, user: `retail_user`)
- WireMock (on a random mapped port)

These are configured using `@TestConfiguration` and Spring Boot 3's [Testcontainers support with `@ServiceConnection`](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers.service-connections).

---

## 🔌 REST API Endpoints

| HTTP URL                                               | Description                                 |
|--------------------------------------------------------|---------------------------------------------|
| `http://localhost:8080/products/fetch?productId=1001`  | Fetch product info from WireMock external API |
| `http://localhost:8080/products/db/1001`               | Fetch product info from PostgreSQL DB       |
| `http://localhost:8080/products/combined/1001`         | Fetch from both DB and WireMock and combine |

---

## 📁 Sample WireMock Stub

WireMock stubs are loaded from:

