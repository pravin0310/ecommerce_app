# 🛒 E-Commerce Microservices Platform

A production-ready, cloud-native e-commerce backend built with **Spring Boot 3.2**, **Spring Cloud**, and **Docker**. The system is designed using microservices architecture with full observability, async messaging, and centralized configuration.

---

## 📌 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Microservices](#microservices)
- [Tech Stack](#tech-stack)
- [Infrastructure & Tools](#infrastructure--tools)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Monitoring & Observability](#monitoring--observability)
- [Message Flow](#message-flow)

---

## 🏗️ Architecture Overview

```
                        ┌─────────────────┐
                        │   Client/Browser │
                        └────────┬────────┘
                                 │
                        ┌────────▼────────┐
                        │   API Gateway   │  ← JWT Auth, Routing
                        │   Port: 8080    │
                        └────────┬────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
    ┌─────────▼──────┐  ┌───────▼────────┐  ┌─────▼──────────┐
    │  User Service  │  │ Product Service│  │  Order Service  │
    │  Port: 8084    │  │  Port: 8085    │  │  Port: 8081     │
    └─────────┬──────┘  └───────┬────────┘  └─────┬──────────┘
              │                  │                  │
         PostgreSQL          PostgreSQL+Redis    PostgreSQL
         (userdb)            (productdb)         (orderdb)
                                                    │
                                              ┌─────▼──────────┐
                                              │     Kafka       │
                                              └─────┬──────────┘
                                                    │
                              ┌─────────────────────┼──────────────────┐
                              │                     │                  │
                   ┌──────────▼─────┐   ┌───────────▼──────┐  ┌───────▼──────────┐
                   │Payment Service │   │Inventory Service │  │Notification Svc  │
                   │  Port: 8082    │   │  Port: 8083      │  │  Port: 8086      │
                   └────────────────┘   └──────────────────┘  └──────────────────┘
```

---

## 🧩 Microservices

| Service | Port | Description | Database |
|---|---|---|---|
| **API Gateway** | 8080 | Single entry point, JWT validation, routing | - |
| **Discovery Server** | 8761 | Eureka service registry | - |
| **Config Server** | 8888 | Centralized configuration management | - |
| **User Service** | 8084 | User registration, login, JWT generation | PostgreSQL (userdb) |
| **Product Service** | 8085 | Product CRUD, Redis caching | PostgreSQL + Redis |
| **Order Service** | 8081 | Order placement, outbox pattern | PostgreSQL (orderdb) |
| **Payment Service** | 8082 | Payment processing via Kafka events | PostgreSQL (paymentdb) |
| **Inventory Service** | 8083 | Stock management via Kafka events | PostgreSQL (inventorydb) |
| **Notification Service** | 8086 | Email/notification via Kafka events | - |

---

## 🛠️ Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.x**
  - Spring Cloud Gateway
  - Spring Cloud Config
  - Spring Cloud Netflix Eureka
  - Spring Cloud OpenFeign
- **Spring Security** with JWT (JJWT 0.11.5)
- **Spring Data JPA** (Hibernate 6)
- **Spring Kafka**
- **Spring Data Redis**
- **Lombok**

### Databases
- **PostgreSQL 15** — 5 separate databases (one per service)
- **Redis 7** — Product caching

### Messaging
- **Apache Kafka** (Confluent 7.4.0)
- **Zookeeper** — Kafka coordination

### Observability
- **Zipkin** — Distributed tracing
- **Prometheus** — Metrics collection
- **Grafana** — Metrics dashboards
- **ELK Stack** (Elasticsearch 8.11, Logstash, Kibana) — Log aggregation

### DevOps
- **Docker** & **Docker Compose** — Containerization
- **Maven 3.9** — Build tool

---

## 🔧 Infrastructure & Tools

### 🔍 Eureka — Service Discovery
**URL:** http://localhost:8761 | **Login:** `eureka` / `password`

All microservices register themselves here. The API Gateway discovers service locations dynamically — no hardcoded URLs needed.

### ⚙️ Config Server — Centralized Configuration
**URL:** http://localhost:8888

Stores all service configurations centrally. Each service fetches its config on startup.
```
http://localhost:8888/product-service/default
http://localhost:8888/order-service/default
```

### 📨 Kafka — Async Messaging
**Kafka UI:** http://localhost:8090

Event-driven communication between services:
- `order-created` → Payment Service
- `payment-processed` → Inventory Service
- `inventory-updated` → Notification Service

### 🔴 Redis — Caching
**Port:** 6379

Product data is cached to reduce database load. Cache TTL is configurable.
```bash
docker exec -it redis redis-cli keys "*"
```

### 🔎 Zipkin — Distributed Tracing
**URL:** http://localhost:9411

Tracks requests across all microservices. Shows latency at each hop.

### 📊 Prometheus — Metrics
**URL:** http://localhost:9090

Scrapes metrics from all services every 15 seconds. Sample queries:
```
http_server_requests_seconds_count
jvm_memory_used_bytes
```

### 📈 Grafana — Dashboards
**URL:** http://localhost:3000 | **Login:** `admin` / `admin`

Visualizes Prometheus metrics. Import dashboard ID `11378` for Spring Boot metrics.

### 🔎 Elasticsearch + Kibana — Log Aggregation
**Kibana URL:** http://localhost:5601

All application logs flow through Logstash → Elasticsearch → Kibana for centralized searching.

---

## 📁 Project Structure

```
ecommerce-microservices/
├── pom.xml                          ← Parent POM (manages all dependencies)
├── docker-compose.yml               ← Full stack orchestration (23 containers)
├── prometheus.yml                   ← Prometheus scrape config
├── logstash.conf                    ← Log pipeline config
│
├── discovery-server/                ← Eureka Server
│   ├── src/main/resources/application.yml
│   └── Dockerfile
│
├── config-server/                   ← Spring Cloud Config Server
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── configurations/         ← Service config files
│   └── Dockerfile
│
├── api-gateway/                     ← Spring Cloud Gateway + JWT filter
│   ├── src/
│   └── Dockerfile
│
├── user-service/                    ← Auth service
│   ├── src/
│   └── Dockerfile
│
├── product-service/                 ← Products + Redis cache
│   ├── src/
│   └── Dockerfile
│
├── order-service/                   ← Orders + Outbox pattern
│   ├── src/
│   └── Dockerfile
│
├── payment-service/                 ← Payment processing
│   ├── src/
│   └── Dockerfile
│
├── inventory-service/               ← Stock management
│   ├── src/
│   └── Dockerfile
│
└── notification-service/            ← Email notifications
    ├── src/
    └── Dockerfile
```

---

## 🚀 Getting Started

### Prerequisites
- Docker Desktop (with WSL2 on Windows)
- Java 17
- Maven 3.9+
- Git

### Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/ecommerce-microservices.git
cd ecommerce-microservices
```

### Start Everything with One Command
```bash
docker-compose up -d
```

This starts all **23 containers** automatically in the correct order:
1. Infrastructure (Zookeeper, Kafka, Redis, PostgreSQL x5)
2. Monitoring (Zipkin, Prometheus, Grafana, ELK)
3. Discovery Server → Config Server → All Services

### Check Status
```bash
docker-compose ps
```

### Stop Everything
```bash
docker-compose down
```

### Rebuild After Code Changes
```bash
docker-compose build --no-cache
docker-compose up -d
```

---

## 🌐 API Endpoints

All requests go through the **API Gateway** at `http://localhost:8080`

### Auth
```http
POST /api/users/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

```http
POST /api/users/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```
→ Returns JWT token

### Products
```http
POST /api/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Laptop",
  "price": 50000,
  "quantity": 10
}
```

```http
GET /api/products
GET /api/products/{id}
PUT /api/products/{id}
DELETE /api/products/{id}
```

### Orders
```http
POST /api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

```http
GET /api/orders
GET /api/orders/{id}
```

---

## 📊 Monitoring & Observability

| Tool | URL | Credentials | Purpose |
|---|---|---|---|
| Eureka | http://localhost:8761 | eureka/password | Service registry |
| Config Server | http://localhost:8888 | - | Config management |
| Kafka UI | http://localhost:8090 | - | Message monitoring |
| Zipkin | http://localhost:9411 | - | Distributed tracing |
| Prometheus | http://localhost:9090 | - | Metrics collection |
| Grafana | http://localhost:3000 | admin/admin | Metrics dashboards |
| Kibana | http://localhost:5601 | - | Log analysis |

---

## 📨 Message Flow

### Order Placement Flow
```
1. POST /api/orders  →  API Gateway  →  Order Service
2. Order Service saves order + outbox event to orderdb
3. Order Service publishes "order-created" event to Kafka
4. Payment Service consumes event → processes payment
5. Payment Service publishes "payment-processed" event
6. Inventory Service consumes event → deducts stock
7. Inventory Service publishes "inventory-updated" event
8. Notification Service consumes event → sends confirmation email
```

### Database Per Service
Each service owns its own database — no shared databases:

```
user-service      → postgres-user:5432     (userdb)
product-service   → postgres-product:5432  (productdb)
order-service     → postgres-order:5432    (orderdb)
payment-service   → postgres-payment:5432  (paymentdb)
inventory-service → postgres-inventory:5432(inventorydb)
```

---

## 🔐 Security

- **JWT Authentication** via API Gateway filter
- All service endpoints protected (except register/login)
- Each PostgreSQL database has isolated credentials
- Non-root Docker users for all service containers
- Spring Security on Discovery Server

---

## 📝 Environment Variables

Key environment variables used in `docker-compose.yml`:

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka registration URL |
| `SPRING_CONFIG_IMPORT` | Config server URL |
| `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT` | Zipkin endpoint |
| `JWT_SECRET` | JWT signing secret (API Gateway) |

---

## 👨‍💻 Author

**Pravin** — Full Stack Java Developer

---

## 📄 License

This project is licensed under the PR License.
