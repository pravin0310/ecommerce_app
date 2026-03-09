# 🚀 E-Commerce Microservices — Startup & Operations Guide

Complete step-by-step guide to run, build, test, and manage the application.

---

## 📋 Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [First Time Setup](#2-first-time-setup)
3. [Daily Startup](#3-daily-startup)
4. [Check Everything is Running](#4-check-everything-is-running)
5. [Build Commands](#5-build-commands)
6. [Run Services Locally (Without Docker)](#6-run-services-locally-without-docker)
7. [Stop the Application](#7-stop-the-application)
8. [View Logs](#8-view-logs)
9. [Test APIs](#9-test-apis)
10. [Monitoring Tools](#10-monitoring-tools)
11. [Database Access](#11-database-access)
12. [Troubleshooting](#12-troubleshooting)
13. [Useful Commands Cheat Sheet](#13-useful-commands-cheat-sheet)

---

## 1. Prerequisites

Make sure these are installed before starting:

| Tool | Version | Download |
|---|---|---|
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| Java JDK | 17 | https://adoptium.net |
| Maven | 3.9+ | Bundled with IntelliJ or https://maven.apache.org |
| Git | Latest | https://git-scm.com |
| pgAdmin (optional) | Latest | https://www.pgadmin.org |
| Postman (optional) | Latest | https://www.postman.com |

### Windows-specific
- Enable **WSL2** in Docker Desktop settings
- Add Maven to PATH:
  ```
  Windows Start → "Edit environment variables for your account"
  → User variables → Path → Edit → New
  → C:\Program Files\JetBrains\IntelliJ IDEA 2025.1\plugins\maven\lib\maven3\bin
  ```

---

## 2. First Time Setup

### Step 1 — Clone the repository
```powershell
git clone https://github.com/YOUR_USERNAME/ecommerce-microservices.git
cd ecommerce-microservices
```

### Step 2 — Validate all pom.xml files
```powershell
mvn validate
```
Should show: `BUILD SUCCESS`

### Step 3 — Build all Docker images (first time takes 10-15 mins)
```powershell
docker-compose build --parallel
```

### Step 4 — Start everything
```powershell
docker-compose up -d
```

### Step 5 — Wait 3-5 minutes for all services to start, then verify
```powershell
docker-compose ps
```

---

## 3. Daily Startup

Every time you start your PC, follow these steps:

### Step 1 — Open Docker Desktop
- Wait until Docker Desktop shows **"Engine running"**
- WSL2 must be enabled

### Step 2 — Navigate to project
```powershell
cd C:\scalar\dev\Project\ecommerce-microservices
```

### Step 3 — Start all containers
```powershell
docker-compose up -d
```

### Step 4 — Wait and check status
```powershell
# Wait 3-4 minutes then check
docker-compose ps
```

All containers should show **Up** or **healthy** status.

### Step 5 — Verify Eureka Dashboard
Open browser: **http://localhost:8761**
- Login: `eureka` / `password`
- All 9 services should be registered

---

## 4. Check Everything is Running

### Check all containers
```powershell
docker-compose ps
```

Expected output — all should show `Up`:
```
NAME                 STATUS
discovery-server     Up (healthy)
config-server        Up (healthy)
user-service         Up
product-service      Up
order-service        Up
payment-service      Up
inventory-service    Up
notification-service Up
api-gateway          Up
postgres-user        Up (healthy)
postgres-product     Up (healthy)
postgres-order       Up (healthy)
postgres-payment     Up (healthy)
postgres-inventory   Up (healthy)
redis                Up (healthy)
kafka                Up (healthy)
zookeeper            Up (healthy)
kafka-ui             Up
zipkin               Up
prometheus           Up
grafana              Up
elasticsearch        Up (healthy)
logstash             Up
kibana               Up
```

### Quick health checks
```powershell
# API Gateway
curl http://localhost:8080/actuator/health

# Discovery Server
curl http://eureka:password@localhost:8761/actuator/health

# Config Server
curl http://localhost:8888/actuator/health
```

### Check specific service is registered in Eureka
Open: **http://localhost:8761** → Login → Look for all services in "Instances currently registered with Eureka"

---

## 5. Build Commands

### Build a single service (after code changes)
```powershell
# Fast build — uses Docker cache (for Java code changes)
docker-compose build user-service

# Full clean build — no cache (for pom.xml changes)
docker-compose build --no-cache user-service
```

### Build all services at once
```powershell
# Build all in parallel (fastest)
docker-compose build --parallel

# Build all with no cache (slowest — use only when needed)
docker-compose build --no-cache
```

### Rebuild and restart a service
```powershell
docker-compose up -d --build user-service
```

### When to use --no-cache

| Change Made | Command |
|---|---|
| Java code changed | `docker-compose build user-service` |
| application.yml changed | `docker-compose build user-service` |
| pom.xml changed | `docker-compose build --no-cache user-service` |
| Dockerfile changed | `docker-compose build --no-cache user-service` |
| First time build | `docker-compose build --parallel` |

---

## 6. Run Services Locally (Without Docker)

Use this when developing/debugging a specific service.

### Prerequisites
- Docker must be running (for databases, Kafka, Redis)
- Start infrastructure containers first

### Step 1 — Start only infrastructure
```powershell
docker-compose up -d zookeeper kafka redis postgres-user postgres-product postgres-order postgres-payment postgres-inventory zipkin
```

### Step 2 — Start Discovery Server (Terminal 1)
```powershell
cd C:\scalar\dev\Project\ecommerce-microservices\discovery-server
mvn spring-boot:run
```
Wait until: `Started DiscoveryServerApplication`

### Step 3 — Start Config Server (Terminal 2)
```powershell
cd C:\scalar\dev\Project\ecommerce-microservices\config-server
mvn spring-boot:run
```
Wait until: `Started ConfigServerApplication`

### Step 4 — Start any service (Terminal 3)
```powershell
# Example: start product-service locally
cd C:\scalar\dev\Project\ecommerce-microservices\product-service
mvn spring-boot:run
```

### Service startup order (must follow this order)
```
1. discovery-server   (Eureka — everything registers here)
2. config-server      (Config — services fetch config from here)
3. user-service
4. product-service
5. order-service
6. payment-service
7. inventory-service
8. notification-service
9. api-gateway        (Start last — depends on all services)
```

---

## 7. Stop the Application

### Stop all containers (keeps data)
```powershell
docker-compose down
```

### Stop all containers and delete all data (fresh start)
```powershell
docker-compose down -v
```
⚠️ Warning: `-v` deletes all database data and volumes!

### Stop a single service
```powershell
docker-compose stop user-service
```

### Restart a single service
```powershell
docker-compose restart user-service
```

---

## 8. View Logs

### View logs of all services
```powershell
docker-compose logs -f
```

### View logs of a specific service
```powershell
docker-compose logs -f user-service
docker-compose logs -f product-service
docker-compose logs -f order-service
docker-compose logs -f api-gateway
docker-compose logs -f config-server
docker-compose logs -f discovery-server
```

### View last 100 lines only
```powershell
docker-compose logs --tail=100 user-service
```

### Search for errors in logs
```powershell
docker-compose logs user-service | Select-String "ERROR"
docker-compose logs user-service | Select-String "Exception"
```

---

## 9. Test APIs

All requests go through **API Gateway** at `http://localhost:8080`

### Register a User
```http
POST http://localhost:8080/api/users/register
Content-Type: application/json

{
  "name": "Pravin",
  "email": "pravin@gmail.com",
  "password": "password123"
}
```

### Login
```http
POST http://localhost:8080/api/users/login
Content-Type: application/json

{
  "email": "pravin@gmail.com",
  "password": "password123"
}
```
→ Copy the JWT token from response

### Create a Product
```http
POST http://localhost:8080/api/products
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "name": "Laptop",
  "price": 50000.00,
  "quantity": 10,
  "description": "High performance laptop"
}
```

### Get All Products
```http
GET http://localhost:8080/api/products
Authorization: Bearer <your-jwt-token>
```

### Place an Order
```http
POST http://localhost:8080/api/orders
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

### Get All Orders
```http
GET http://localhost:8080/api/orders
Authorization: Bearer <your-jwt-token>
```

### Using curl (PowerShell)
```powershell
# Register
curl -X POST http://localhost:8080/api/users/register `
  -H "Content-Type: application/json" `
  -d '{"name":"Pravin","email":"pravin@gmail.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/users/login `
  -H "Content-Type: application/json" `
  -d '{"email":"pravin@gmail.com","password":"password123"}'
```

---

## 10. Monitoring Tools

### Eureka — Service Registry
- **URL:** http://localhost:8761
- **Login:** `eureka` / `password`
- **Purpose:** See all registered services and their status

### Config Server
- **URL:** http://localhost:8888
- **Check service config:** http://localhost:8888/user-service/default

### Kafka UI
- **URL:** http://localhost:8090
- **Purpose:** View Kafka topics, messages, consumer groups
- **Topics to watch:** `order-created`, `payment-processed`, `inventory-updated`

### Zipkin — Distributed Tracing
- **URL:** http://localhost:9411
- **Purpose:** Trace requests across services
- **How to use:** Click "Run Query" → Select service → See traces

### Prometheus — Metrics
- **URL:** http://localhost:9090
- **Useful queries:**
  ```
  http_server_requests_seconds_count
  jvm_memory_used_bytes
  process_cpu_usage
  ```

### Grafana — Dashboards
- **URL:** http://localhost:3000
- **Login:** `admin` / `admin`
- **Setup:**
  1. Add data source → Prometheus → URL: `http://prometheus:9090`
  2. Import dashboard → ID: `11378` → Spring Boot metrics

### Kibana — Log Search
- **URL:** http://localhost:5601
- **Purpose:** Search logs from all services

---

## 11. Database Access

Connect using **pgAdmin** or any PostgreSQL client:

| Service | Host | Port | Database | Username | Password |
|---|---|---|---|---|---|
| user-service | localhost | 5432 | userdb | postgres | password |
| product-service | localhost | 5433 | productdb | postgres | password |
| order-service | localhost | 5434 | orderdb | postgres | password |
| payment-service | localhost | 5435 | paymentdb | postgres | password |
| inventory-service | localhost | 5436 | inventorydb | postgres | password |

### Connect via command line
```powershell
# Connect to userdb
docker exec -it postgres-user psql -U postgres -d userdb

# List all tables
\dt

# Query users
SELECT * FROM users;

# Exit
\q
```

### Redis
```powershell
# Connect to Redis
docker exec -it redis redis-cli

# Check all cached keys
keys *

# Check connection
ping
```

---

## 12. Troubleshooting

### Problem: Docker containers not starting
```powershell
# Check what's wrong
docker-compose ps
docker-compose logs <service-name>
```

### Problem: Port already in use
```powershell
# Find what's using the port
netstat -ano | findstr :8080

# Kill the process
taskkill /PID <PID> /F
```

### Problem: Services not showing in Eureka
- Wait 2-3 minutes after startup — Eureka registration takes time
- Check service logs: `docker-compose logs -f user-service`
- Verify `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` env variable is set

### Problem: Database connection refused
```powershell
# Check if postgres container is running
docker-compose ps postgres-user

# Start it if not running
docker-compose up -d postgres-user

# Test connection
docker exec -it postgres-user psql -U postgres -d userdb -c "\dt"
```

### Problem: Build taking too long
```powershell
# Only rebuild changed service
docker-compose build user-service

# Use parallel build for all services
docker-compose build --parallel
```

### Problem: Out of Docker disk space
```powershell
# Remove unused images and cache
docker builder prune -a -f
docker system prune -f
```

### Problem: Config server not found
- Make sure discovery-server is healthy first
- Check: `docker-compose logs config-server`
- Verify env: `SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888`

### Problem: Kafka messages not flowing
```powershell
# Check Kafka is healthy
docker-compose ps kafka

# View Kafka logs
docker-compose logs kafka

# Open Kafka UI to inspect topics
# http://localhost:8090
```

---

## 13. Useful Commands Cheat Sheet

```powershell
# ─── STARTUP ───────────────────────────────────────
docker-compose up -d                          # Start all services
docker-compose up -d user-service             # Start single service
docker-compose up -d --build user-service     # Rebuild and start

# ─── STATUS ────────────────────────────────────────
docker-compose ps                             # Show all containers
docker-compose ps user-service                # Show single container

# ─── LOGS ──────────────────────────────────────────
docker-compose logs -f                        # All logs (live)
docker-compose logs -f user-service           # Single service logs
docker-compose logs --tail=50 user-service    # Last 50 lines

# ─── BUILD ─────────────────────────────────────────
docker-compose build user-service             # Build with cache
docker-compose build --no-cache user-service  # Full rebuild
docker-compose build --parallel               # Build all in parallel

# ─── STOP ──────────────────────────────────────────
docker-compose down                           # Stop, keep data
docker-compose down -v                        # Stop, delete data
docker-compose stop user-service              # Stop single service
docker-compose restart user-service           # Restart single service

# ─── CLEANUP ───────────────────────────────────────
docker builder prune -a -f                    # Clear build cache
docker system prune -f                        # Remove unused resources
docker images -q | ForEach-Object { docker rmi $_ -f }  # Remove all images

# ─── DATABASE ──────────────────────────────────────
docker exec -it postgres-user psql -U postgres -d userdb    # Connect userdb
docker exec -it postgres-product psql -U postgres -d productdb
docker exec -it redis redis-cli ping          # Test Redis

# ─── MAVEN (local development) ─────────────────────
mvn validate                                  # Validate all pom.xml
mvn spring-boot:run                           # Run service locally
mvn clean package -DskipTests                 # Build jar
```

---

## 📞 Service URLs Quick Reference

| Service | URL | Notes |
|---|---|---|
| API Gateway | http://localhost:8080 | All API requests go here |
| Eureka | http://localhost:8761 | eureka/password |
| Config Server | http://localhost:8888 | - |
| Kafka UI | http://localhost:8090 | - |
| Zipkin | http://localhost:9411 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin |
| Kibana | http://localhost:5601 | - |
| User Service | http://localhost:8084 | - |
| Product Service | http://localhost:8085 | - |
| Order Service | http://localhost:8081 | - |
| Payment Service | http://localhost:8082 | - |
| Inventory Service | http://localhost:8083 | - |
| Notification Service | http://localhost:8086 | - |
