# Bloom — Smart Career & Learning Platform for Students

> Match your skills to real jobs. Know exactly what to learn next.

---

## Team

| Name |
|---|
| Zainab Farih |
| Assia Rguibi |

> Course: Développement de Plateformes — Prof. EL HAMLAOUI Mahmoud — 2025/2026

---

## Table of Contents

1. [Introduction](#introduction)
2. [Actors & Roles](#actors--roles)
3. [Features](#features)
4. [Technology Choices](#technology-choices)
5. [Architecture](#architecture)
6. [Live URLs & Access Points](#live-urls--access-points)
7. [Running the Project Locally](#running-the-project-locally)
8. [Security](#security)
9. [Testing](#testing)
10. [CI/CD & Deployment](#cicd--deployment)
11. [Monitoring & Observability](#monitoring--observability)
12. [API Documentation](#api-documentation)
13. [Challenges](#challenges)
14. [Conclusion](#conclusion)
15. [Demo](#demo)

---

## Introduction

Students usually get stuck on two questions: *what should I learn next?* and *which jobs am I actually qualified for?* Bloom answers both in one place.

A student can browse a catalog of tech roles, pick the skills they want to learn, and build a personal learning plan. They can also upload their CV — the platform extracts their skills and matches them against real job offers, showing which skills they already have and which ones are missing.

The project is built as a set of Spring Boot microservices, with a React frontend, full CI/CD, container deployment on Oracle Cloud (Kubernetes), and a complete monitoring stack.

---

## Actors & Roles

| Actor | Role |
|---|---|
| **Student** | Uploads a CV, explores roles, builds a learning plan, tracks progress |
| **Admin** | Manages the job cache and monitors the platform |
| **AI (Gemini)** | Extracts skills from CVs, analyzes CV quality, generates roadmaps |
| **Jobs API** | External source of real job and internship offers |

---

## Features

- **Tech role catalog** — browse roles (Software Engineer, Data Analyst, DevOps, etc.), each with required skills and a learning order.
- **CV upload & skill extraction** — upload a PDF, the AI extracts skills, experience and education.
- **Job matching** — match extracted skills against real offers, ranked by compatibility, with a clear skill-gap view.
- **Skill gap analysis** — see exactly which skills are missing for a role or an offer, and add them to the learning plan.
- **Personal learning plan** — a single place to manage skills to learn, with roadmaps and progress tracking.
- **CV improvement suggestions** — AI feedback on structure and content, with a quality score.
- **Student profile** — verified skills, skills in progress, and roadmap progress.

---

## Technology Choices

| Layer | Choice | Why |
|---|---|---|
| **Backend** | Spring Boot 3 + Spring Cloud | Mature microservices ecosystem (config, discovery, gateway) |
| **Frontend** | React 19 + TypeScript (Vite) | Fast SPA, type safety, modern tooling |
| **Database** | PostgreSQL (DB per service) | Relational, isolated per service |
| **Persistence** | Spring Data JPA + Flyway migrations | Repositories + versioned schema/seed scripts |
| **Security** | Spring Security + JWT (BCrypt) | Stateless auth validated at the gateway |
| **AI** | Google Gemini | Skill extraction, CV analysis, roadmap generation |
| **Cache** | Redis | Caches job offers to limit external API calls |
| **DevOps** | Docker · GitHub Actions · Kubernetes (OKE) · Terraform | Reproducible builds and deployments |
| **Monitoring** | Prometheus · Grafana · Loki · Zipkin | Metrics, logs and distributed tracing |

Frontend libraries: Axios (with token refresh interceptor), React Router, Zustand, React Query, React Hook Form + Zod, CSS Modules, lucide-react.

---

## Architecture

Bloom uses a microservices architecture. The frontend talks only to the **API Gateway**, which validates the JWT and routes requests. Each service registers with **Eureka** (discovery) and pulls its configuration from the **Config Server**. Each business service owns its own database — no service touches another service's data directly.

### Business services

| Service | Port | Database | Description |
|---|---|---|---|
| **auth-service** | 8081 | bloom_auth | Registration, login, JWT, password & user management |
| **cv-service** | 8082 | bloom_cv | CV upload (PDF), AI skill extraction, CV analysis |
| **job-service** | 8083 | bloom_jobs | Job fetching, matching, skill-gap detection, Redis cache |
| **roadmap-service** | 8085 | bloom_roadmap | AI-generated learning roadmaps |

### Infrastructure services

| Service | Port | Description |
|---|---|---|
| **api-gateway** | 8080 | Single entry point, routing + JWT validation |
| **discovery-server** | 8761 | Eureka service registry |
| **config-server** | 8888 | Centralized configuration (from `config-repo/`) |
| **admin-server** | 8090 | Spring Boot Admin — service health dashboard |

### Observability

| Tool | Port | Purpose |
|---|---|---|
| **Prometheus** | 9090 | Metrics collection |
| **Grafana** | 3001 | Dashboards |
| **Loki** | 3100 | Log aggregation |
| **Zipkin** | 9411 | Distributed tracing |
| **Redis** | 6379 | Job cache |

---

## Live URLs & Access Points

**Deployed app (Oracle Cloud):** http://140.238.123.230

Local (via Docker Compose):

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Eureka (discovery) | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| Spring Boot Admin | http://localhost:8090 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 |
| Zipkin | http://localhost:9411 |
| Loki | http://localhost:3100 |
| Swagger — cv-service | http://localhost:8082/swagger-ui.html |
| Swagger — job-service | http://localhost:8083/swagger-ui.html |

---

## Running the Project Locally

**Prerequisites:** Docker & Docker Compose, JDK (for building services), Node.js (for the frontend).

1. Copy the environment template and fill in your values (database URLs, JWT secret, API keys):

   ```bash
   cp .env.example .env
   ```

2. Start the whole stack:

   ```bash
   docker compose up --build
   ```

3. Open the frontend at http://localhost:5173. 

Services read their configuration from the Config Server (`config-repo/`) and register with Eureka. Databases are external managed PostgreSQL instances configured through `.env`.

---

## Security

- **Passwords** are hashed with **BCrypt** (strength 12) — never stored in plain text.
- **Authentication** is stateless using **JWT**. The gateway validates the token; downstream services trust the gateway through an internal shared secret.
- **Role-based access** with `@EnableMethodSecurity` and `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints (e.g. cache eviction, user listing).
- **Input validation** with Bean Validation (`@Valid`) on request bodies.
- **Parameterized queries** through Spring Data JPA — no string-built SQL.
- **No secrets in source code** — everything sensitive is injected via environment variables.

---

## Testing

Unit and integration tests use **JUnit 5**, **Mockito** and **MockMvc**, with **JaCoCo** for coverage reports.

- **cv-service** — tests for controller, service, mapper, security filter, PDF extraction and skill extraction.
- **job-service** — tests for controller, services (matching, saved jobs, skill extraction), mapper, entity and security filter.

Run tests for a service:

```bash
cd services/cv-service
./mvnw test
```

Coverage reports are generated under `target/site/jacoco/`.

---

## CI/CD & Deployment

- **CI** (`.github/workflows/ci-*.yaml`) runs on pull requests: compile, run tests, and verify the build per service.
- **CD** (`.github/workflows/cd-*.yaml`) runs on merge to `main`: builds a Docker image, pushes it to **Oracle Container Registry (OCIR)**, and deploys to **Oracle Kubernetes Engine (OKE)** via `kubectl set image` + rollout.
- **Kubernetes manifests** live in `k8s/` (Kustomize base + prod overlay).
- **Infrastructure as code** lives in `terraform/oci/` — provisions the OKE cluster and network on Oracle Cloud.

---

## Monitoring & Observability

A full observability stack runs alongside the services:

- **Prometheus** scrapes metrics from each service (Spring Actuator).
- **Grafana** displays dashboards (gateway + platform overview).
- **Loki + Promtail** aggregate logs from all containers.
- **Zipkin** traces requests across services.
- **Spring Boot Admin** gives a live view of every service's health.

---

## API Documentation

Each service (cv, job) exposes interactive **Swagger / OpenAPI** docs at `/swagger-ui.html`. UML diagrams (use case, class, sequence) are in `docs/uml/`.

---

## Challenges

- **Service-to-service communication** — keeping each database isolated meant matching had to go through REST calls (Feign clients) instead of shared tables, with a fallback to stay resilient when a service is down.
- **Stateless security across services** — validating the JWT once at the gateway and letting downstream services trust it, without re-validating everywhere, took a few iterations to get right.
- **Real deployment** — getting the full stack running on Oracle Kubernetes (OKE) with automated CI/CD, image registry and rolling updates was the hardest part.
- **AI integration** — making skill extraction and CV analysis reliable enough on messy real CVs while keeping calls asynchronous to avoid blocking the API.

---

## Conclusion

Bloom delivers a working microservices platform that connects what a student knows to what the job market wants. It covers the full path: requirements, design, secured REST APIs, a real database with migrations, tested services, automated CI/CD, and a monitored production deployment on the cloud. The architecture leaves room to grow — new services can be added without touching the existing ones.

---

> *"Don't just apply for jobs. Know exactly what you need to get there."*
