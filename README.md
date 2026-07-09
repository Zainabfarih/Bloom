<div align="center">

# 🌱 Bloom

### Smart Career & Learning Platform for Students

Match your skills to real jobs. Know exactly what to learn next.

[![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.1.1-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![React](https://img.shields.io/badge/React-19.2-61DAFB?logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite&logoColor=white)](https://vitejs.dev/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-OKE-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![Terraform](https://img.shields.io/badge/Terraform-OCI-844FBA?logo=terraform&logoColor=white)](https://www.terraform.io/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![License](https://img.shields.io/badge/License-Academic%20Project-lightgrey)](#)

**[Live Demo](http://140.238.123.230)** · **[Zainab Farih](https://www.linkedin.com/in/zainab-farih/)** · **[Assia Rguibi](https://www.linkedin.com/in/assia-rguibi-a6318732b/)**

Made with ❤️ by **Zainab Farih** and **Assia Rguibi**

</div>

---

## Table of Contents

- [About the Project](#about-the-project)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Microservices](#microservices)
- [Getting Started](#getting-started)
- [Testing & Quality](#testing--quality)
- [Security](#security)
- [API Documentation](#api-documentation)
- [Infrastructure & Deployment](#infrastructure--deployment)
- [CI/CD Pipeline](#cicd-pipeline)
- [Observability](#observability)
- [Accessing the Live Deployment](#accessing-the-live-deployment)
- [Contact](#contact)

---

## About the Project

Students usually get stuck on two questions: *what should I learn next?* and *which jobs am I actually qualified for?* **Bloom** answers both in one place.

A student creates an account, uploads their CV, and the platform extracts their skills using AI. Those skills are matched against real job offers, surfacing a clear compatibility score and the exact skills that are missing. From any skill gap, the student can generate a personalized, AI-built learning roadmap and track their progress to completion.

Bloom is built as a set of **Spring Boot microservices** behind a single **API Gateway**, with a **React** frontend — shipping with centralized configuration, service discovery, JWT security, a full CI/CD pipeline, container orchestration on **Oracle Kubernetes Engine (OKE)**, infrastructure provisioned with **Terraform**, and a complete observability stack.

| Actor | Role |
|---|---|
| **Student** | Registers, verifies email, uploads a CV, explores jobs, generates roadmaps, tracks progress |
| **Admin** | Manages users and the job cache, monitors the platform |
| **AI · Google Gemini** | Extracts skills from CVs, analyzes CV quality, generates learning roadmaps |
| **Jobs API · SerpApi** | External source of real job and internship offers |

---

## Key Features

- 🔐 **Account & email verification** — mandatory email verification, no account usable until confirmed; password reset by email
- 📄 **CV upload & skill extraction** — upload a PDF or fill a manual form; AI extracts skills, experience and education
- 🎯 **Job matching** — real offers ranked by a compatibility score, with a clear skill-gap view
- 📊 **Skill gap analysis** — see exactly which skills are missing for a given offer
- 🗺️ **AI learning roadmaps** — step-by-step roadmap for a target job, with curated resources and progress tracking
- 📝 **CV analysis** — AI feedback on structure and content with an ATS-style quality score
- 🛠️ **Admin dashboard** — user management and platform statistics

---

## Architecture

```
                                   ┌────────────────────┐
                                   │      Frontend       │
                                   │   React + Vite      │
                                   └──────────┬───────────┘
                                              │ HTTPS
                                   ┌──────────▼───────────┐
                                   │      API Gateway      │
                                   │  routing + JWT check   │
                                   └──────────┬───────────┘
                     ┌───────────────┬────────┼────────┬───────────────┐
                     ▼               ▼        ▼        ▼               ▼
              ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────────────┐
              │auth-service│ │ cv-service │ │job-service │ │ roadmap-service   │
              └────────────┘ └────────────┘ └────────────┘ └──────────────────┘
                     │               │             │                  │
                     └───────────────┴──────┬──────┴──────────────────┘
                                            ▼
                          Eureka (discovery) · Config Server · Redis
```

Every business service registers with **Eureka** and pulls its configuration from the **Config Server**. Each service owns its own database — no service reads another service's tables directly. The frontend never talks to a business service directly; every request goes through the **API Gateway**, which validates the JWT and forwards a trusted internal secret downstream.

Full UML (use-case, class, sequence, deployment) is available in [`docs/uml/`](./docs/uml).

---

## Tech Stack

| Layer | Technologies |
|---|---|
| **Backend** | Java 25 · Spring Boot 4.0 · Spring Cloud 2025.1.1 (Config, Eureka, Gateway) |
| **Frontend** | React 19.2 · TypeScript 6.0 · Vite 8 · React Router 7 · Zustand 5 · React Query 5 · React Hook Form 7 + Zod 4 · Axios 1.17 |
| **Database** | PostgreSQL — one database per service (Supabase / Azure, managed) |
| **Persistence** | Spring Data JPA + Flyway migrations |
| **Connection Pool** | HikariCP |
| **Security** | Spring Security + JWT (JJWT 0.12.6), BCrypt password hashing |
| **AI** | Google Gemini — skill extraction, CV analysis, roadmap generation |
| **Cache** | Redis — job offers |
| **Containers** | Docker (multi-stage builds), nginx for the frontend |
| **Orchestration** | Kubernetes (Oracle OKE) + Kustomize |
| **IaC** | Terraform (Oracle Cloud Infrastructure) |
| **CI/CD** | GitHub Actions + Oracle Container Registry (OCIR) |
| **Observability** | Prometheus, Grafana, Loki + Promtail, Zipkin |

---

## Repository Structure

```
development-platform-bloom/
├── frontend/             # React + TypeScript (Vite) single-page app
├── services/             # Business microservices (Spring Boot)
│   ├── auth-service/
│   ├── cv-service/
│   ├── job-service/
│   └── roadmap-service/
├── infrastructure/       # Platform services + monitoring stack
│   ├── api-gateway/
│   ├── discovery-server/
│   ├── config-server/
│   ├── admin-server/
│   └── monitoring/       # Prometheus, Grafana, Loki, Promtail configs
├── config-repo/          # Centralized config files served by the Config Server
├── k8s/                  # Kubernetes manifests (Kustomize base + overlays)
├── terraform/oci/        # Infrastructure as Code (Oracle Cloud: VCN + OKE)
├── docs/uml/             # UML diagrams (.puml / .mmd)
├── .github/workflows/    # CI/CD pipelines (one CI + one CD per service)
└── docker-compose.yml    # Full local stack
```

---

## Microservices

### Business services

| Service | Port | Database | Responsibility |
|---|---|---|---|
| `auth-service` | `8081` | PostgreSQL (Supabase) | Registration, email verification, login, JWT issuing/refresh, password reset, user & admin management |
| `cv-service` | `8082` | `bloom_cv` (Azure) | CV upload (PDF) and manual CV, AI skill extraction, AI CV analysis (ATS score) |
| `job-service` | `8083` | `bloom_job` (Azure) | Job search & fetch, skill matching, skill-gap detection, saved jobs, Redis caching |
| `roadmap-service` | `8085` | PostgreSQL (Supabase) | AI-generated learning roadmaps, steps & resources, progress tracking |

### Infrastructure services

| Service | Port | Responsibility |
|---|---|---|
| `api-gateway` | `8080` | Single entry point — routing + JWT validation, forwards an internal trust secret downstream |
| `discovery-server` | `8761` | Eureka service registry |
| `config-server` | `8888` | Centralized configuration served from `config-repo/` |
| `admin-server` | `8090` | Spring Boot Admin — live service health dashboard |

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 25 (for local backend development)
- Node.js 18+ (for local frontend development)

### Run the full stack locally

```bash
git clone <repository-url>
cd development-platform-bloom
cp .env.example .env      # fill in DB URLs, JWT secret, API keys
docker compose up --build
```

The stack spins up every service, the gateway, discovery, config server, admin server, Redis and the monitoring stack on a shared Docker network with health checks.

| Environment | URL |
|---|---|
| Frontend (Vite dev) | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Production | http://140.238.123.230 |

---

## Testing & Quality

Tests use **JUnit 5**, **Mockito** and **MockMvc**, with **JaCoCo** enforcing an **80% coverage threshold** on the verify phase.

| Service | Coverage highlights |
|---|---|
| `auth-service` | 109 tests across controllers, services, security (JWT filter/service), mapper, entity and exception handling |
| `cv-service` | Controller, service, mapper, security filter, PDF/skill extraction |
| `job-service` | Controller, matching & saved-jobs services, skill extraction, mapper, entity, security filter |
| `roadmap-service` | Controller, service and roadmap-generation logic |

```bash
cd services/auth-service
./mvnw test
```

Coverage reports are generated under each service's `target/site/jacoco/`.

---

## Security

- **BCrypt** password hashing (strength 12) — no plain-text passwords
- **Stateless JWT authentication** — the gateway validates the token; downstream services trust the gateway via an internal shared secret
- **Mandatory email verification** before an account can log in
- **Role-based access control** with `@EnableMethodSecurity` and `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints
- **Input validation** with Bean Validation (`@Valid`); consistent error responses that don't leak technical details
- **Parameterized queries** through Spring Data JPA — no string-built SQL
- **No secrets in source code** — everything injected via environment variables / Kubernetes Secrets

---

## API Documentation

| Type | Tooling | Location |
|---|---|---|
| REST API | Swagger / OpenAPI (springdoc) | `/swagger-ui.html` on each business service |
| Code | Javadoc (`mvn javadoc:javadoc`) | `target/reports/apidocs/` per service |
| Design | PlantUML diagrams | [`docs/uml/`](./docs/uml) — use case, class, sequence, deployment |

Swagger UI (local):

- `auth-service` → http://localhost:8081/swagger-ui.html
- `cv-service` → http://localhost:8082/swagger-ui.html
- `job-service` → http://localhost:8083/swagger-ui.html
- `roadmap-service` → http://localhost:8085/swagger-ui.html

> In production, services sit behind the gateway and are not individually exposed.

---

## Infrastructure & Deployment

### Containerization

Every service and the frontend is containerized with **Docker**, using multi-stage builds to keep images small. The frontend is served by **nginx**, which proxies `/api` to the gateway and provides SPA fallback routing.

### Orchestration

Production workloads run on **Kubernetes (Oracle OKE)**, managed with **Kustomize**:

- [`k8s/base/`](./k8s/base) — Deployments, Services and ConfigMaps for every component, all in the `bloom` namespace
- [`k8s/overlays/`](./k8s/overlays) — environment-specific overlays (`local`, `prod`)
- Centralized configuration mounted into the Config Server via a `configMapGenerator` built from `config-repo/`
- The frontend is exposed through a **LoadBalancer** Service backed by the OCI load balancer

### Infrastructure as Code

The cloud infrastructure is fully provisioned with **Terraform** ([`terraform/oci/`](./terraform/oci)) on **Oracle Cloud Infrastructure**:

- A dedicated **VCN** (`10.0.0.0/16`) with a public subnet for the Kubernetes API endpoint, a public subnet for the load balancer, and a private subnet for worker nodes (no public IPs)
- **Internet Gateway**, **NAT Gateway** and **Service Gateway**, with security lists opening only the required ports
- An OKE **BASIC_CLUSTER** with a managed node pool on flexible shapes (ARM/AMD)

```bash
cd terraform/oci
terraform init
terraform apply
```

### Production topology

- **Only the frontend is publicly exposed**, fronted by the OCI Load Balancer
- All other components — API Gateway, Eureka, Config Server, Admin Server, and the full observability stack — remain **internal** (`ClusterIP`)
- **Worker nodes** live in a private subnet, reaching the internet only via the NAT Gateway
- Databases are **managed PostgreSQL** (Supabase / Azure) over TLS; **Redis** provides the job cache

---

## CI/CD Pipeline

Automated with **GitHub Actions** — one CI and one CD workflow per service ([`.github/workflows/`](./.github/workflows)):

- **CI** (`ci-*.yaml`) runs on pull requests through staged levels: **install → lint/compile → test → build**, failing the build if tests or coverage thresholds don't pass
- **CD** (`cd-*.yaml`) runs on merge to `main`: builds a Docker image, pushes it to **Oracle Container Registry (OCIR)**, and deploys to **OKE** via `kubectl set image` + rollout, using a shared reusable workflow (`cd-reusable.yaml`)
- Path filters ensure only the affected service is rebuilt on a given change

---

## Observability

| Tool | Local Port | Purpose |
|---|---|---|
| **Prometheus** | `9090` | Scrapes metrics from each service via Spring Boot Actuator |
| **Grafana** | `3001` | Dashboards for the gateway and platform overview |
| **Loki + Promtail** | `3100` | Aggregates and queries logs from all containers |
| **Zipkin** | `9411` | Distributed tracing across services |
| **Spring Boot Admin** | `8090` | Live health view of every registered service |

Grafana visualizes request rates, latencies and JVM metrics; Zipkin traces a single request as it travels gateway → service → service.

> In production these dashboards are **internal only** and reached via `kubectl port-forward` — only the frontend is publicly exposed.

---

## Accessing the Live Deployment

Internal dashboards and the deployed services' API docs are reached through `kubectl port-forward` — the URL stays `localhost`, but the data comes from the **production** cluster. Each command runs in its own terminal (`Ctrl+C` to stop).

<details>
<summary><b>Platform dashboards</b></summary>

```bash
# Eureka (discovery)
kubectl port-forward svc/discovery-server 8761:8761 -n bloom
# -> http://localhost:8761

# Spring Boot Admin
kubectl port-forward svc/admin-server 8090:8090 -n bloom
# -> http://localhost:8090

# Grafana (service listens on 3000 inside the cluster)
kubectl port-forward svc/grafana 3001:3000 -n bloom
# -> http://localhost:3001

# Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n bloom
# -> http://localhost:9090

# Zipkin
kubectl port-forward svc/zipkin 9411:9411 -n bloom
# -> http://localhost:9411

# API Gateway
kubectl port-forward svc/api-gateway 8080:8080 -n bloom
# -> http://localhost:8080
```

</details>

<details>
<summary><b>Swagger / OpenAPI of deployed services</b></summary>

```bash
# auth-service
kubectl port-forward svc/auth-service 8081:8081 -n bloom
# -> http://localhost:8081/swagger-ui.html

# cv-service
kubectl port-forward svc/cv-service 8082:8082 -n bloom
# -> http://localhost:8082/swagger-ui.html

# job-service
kubectl port-forward svc/job-service 8083:8083 -n bloom
# -> http://localhost:8083/swagger-ui.html

# roadmap-service
kubectl port-forward svc/roadmap-service 8085:8085 -n bloom
# -> http://localhost:8085/swagger-ui.html
```

</details>

---

## Contact

<div align="center">

[![Zainab Farih](https://img.shields.io/badge/Zainab%20Farih-LinkedIn-0A66C2?logo=linkedin&logoColor=white)](https://www.linkedin.com/in/zainab-farih/)
[![Assia Rguibi](https://img.shields.io/badge/Assia%20Rguibi-LinkedIn-0A66C2?logo=linkedin&logoColor=white)](https://www.linkedin.com/in/assia-rguibi-a6318732b/)

</div>

---

<div align="center">

*"Don't just apply for jobs. Know exactly what you need to get there."*

</div>
