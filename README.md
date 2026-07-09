# Bloom — Smart Career & Learning Platform for Students

> Match your skills to real jobs. Know exactly what to learn next.

---

## Team

- Zainab Farih
- Assia Rguibi

---

## Table of Contents

1. [Introduction](#introduction)
2. [Actors & Roles](#actors--roles)
3. [Features](#features)
4. [Repository Structure](#repository-structure)
5. [Technology Stack](#technology-stack)
6. [Microservices](#microservices)
7. [Testing](#testing)
8. [Security](#security)
9. [Documentation](#documentation)
10. [Containerization](#containerization)
11. [Orchestration](#orchestration)
12. [Infrastructure as Code](#infrastructure-as-code)
13. [CI/CD](#cicd)
14. [Observability](#observability)
15. [Deployment](#deployment)
16. [Conclusion](#conclusion)

---

## Introduction

Students usually get stuck on two questions: *what should I learn next?* and *which jobs am I actually qualified for?* **Bloom** answers both in one place.

A student creates an account, uploads their CV, and the platform extracts their skills using AI. Those skills are then matched against real job offers, showing a clear compatibility score and the exact skills that are missing. From any skill gap, the student can generate a personalized, AI-built learning roadmap and track their progress to completion.

The project is built as a set of **Spring Boot microservices** behind a single **API Gateway**, with a **React** frontend. It ships with centralized configuration, service discovery, JWT security, a full CI/CD pipeline, container orchestration on **Oracle Kubernetes Engine (OKE)**, infrastructure provisioned with **Terraform**, and a complete monitoring stack.

---

## Actors & Roles

| Actor | Role |
|---|---|
| **Student** | Registers, verifies email, uploads a CV, explores jobs, generates roadmaps, tracks progress |
| **Admin** | Manages users and the job cache, monitors the platform |
| **AI (Google Gemini)** | Extracts skills from CVs, analyzes CV quality, generates learning roadmaps |
| **Jobs API (SerpApi)** | External source of real job and internship offers |

---

## Features

- **Account & email verification** — registration with mandatory email verification; no account is usable until verified. Password reset by email.
- **CV upload & skill extraction** — upload a PDF or fill a manual form; the AI extracts skills, experience and education.
- **Job matching** — match extracted skills against real offers, ranked by a compatibility score, with a clear skill-gap view.
- **Skill gap analysis** — see exactly which skills are missing for a given offer.
- **AI learning roadmaps** — generate a step-by-step roadmap for a target job, with curated resources and progress tracking.
- **CV analysis** — AI feedback on CV structure and content with an ATS-style quality score.
- **Admin dashboard** — user management and platform statistics.

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

## Technology Stack

| Layer | Choice |
|---|---|
| **Backend** | Spring Boot + Spring Cloud (Config, Eureka, Gateway) |
| **Frontend** | React + TypeScript, Vite, React Router, Zustand, React Query, React Hook Form + Zod, Axios |
| **Database** | PostgreSQL — one database per service (managed: Supabase / Azure) |
| **Persistence** | Spring Data JPA + Flyway migrations (schema + seed) |
| **Connection pool** | HikariCP |
| **Security** | Spring Security + JWT, BCrypt password hashing |
| **AI** | Google Gemini (skill extraction, CV analysis, roadmap generation) |
| **Cache** | Redis (job offers) |
| **Containers** | Docker (multi-stage builds), nginx for the frontend |
| **Orchestration** | Kubernetes (Oracle OKE) + Kustomize |
| **IaC** | Terraform (Oracle Cloud Infrastructure) |
| **CI/CD** | GitHub Actions + Oracle Container Registry (OCIR) |
| **Observability** | Prometheus, Grafana, Loki + Promtail, Zipkin |

---

## Microservices

The frontend talks **only** to the API Gateway, which validates the JWT and routes requests. Each service registers with **Eureka** and pulls its configuration from the **Config Server**. Each business service owns its own database — no service reads another service's tables directly.

### Business services

| Service | Port | Database | Content |
|---|---|---|---|
| **auth-service** | 8081 | PostgreSQL (Supabase) | Registration, email verification, login, JWT issuing/refresh, password reset, user & admin management |
| **cv-service** | 8082 | `bloom_cv` (Azure) | CV upload (PDF) and manual CV, AI skill extraction, AI CV analysis (ATS score) |
| **job-service** | 8083 | `bloom_job` (Azure) | Job search & fetch, skill matching, skill-gap detection, saved jobs, Redis caching |
| **roadmap-service** | 8085 | PostgreSQL (Supabase) | AI-generated learning roadmaps, steps & resources, progress tracking |

### Infrastructure services

| Service | Port | Content |
|---|---|---|
| **api-gateway** | 8080 | Single entry point — routing + JWT validation, forwards an internal trust secret to downstream services |
| **discovery-server** | 8761 | Eureka service registry |
| **config-server** | 8888 | Centralized configuration served from `config-repo/` |
| **admin-server** | 8090 | Spring Boot Admin — live service health dashboard |

---

## Testing

Tests use **JUnit 5**, **Mockito** and **MockMvc**, with **JaCoCo** for coverage (enforced 80% threshold on the verify phase).

- **auth-service** — 109 tests across controllers, services, security (JWT filter/service), mapper, entity and exception handling.
- **cv-service** — controller, service, mapper, security filter, PDF/skill extraction.
- **job-service** — controller, matching & saved-jobs services, skill extraction, mapper, entity, security filter.
- **roadmap-service** — controller, service and roadmap-generation logic.

Run tests for a service:

```bash
cd services/auth-service
./mvnw test
```

Coverage reports are generated under each service's `target/site/jacoco/`.

---

## Security

- **Password hashing** with **BCrypt** (strength 12) — no plain-text passwords.
- **Stateless JWT authentication** — the gateway validates the token; downstream services trust the gateway through an internal shared secret.
- **Mandatory email verification** — accounts cannot log in until the email is verified.
- **Role-based access control** with `@EnableMethodSecurity` and `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints.
- **Input validation** with Bean Validation (`@Valid`) on request bodies; consistent error responses without leaking technical details.
- **Parameterized queries** through Spring Data JPA — no string-built SQL.
- **No secrets in source code** — all secrets injected via environment variables / Kubernetes Secrets.

---

## Documentation

| Type | Tooling | Where |
|---|---|---|
| **REST API** | Swagger / OpenAPI (springdoc) | `/swagger-ui.html` on each business service |
| **Code** | Javadoc (`mvn javadoc:javadoc`) | `target/reports/apidocs/` per service |
| **Design** | PlantUML diagrams | `docs/uml/` (use case, class, sequence, deployment) |

Swagger UI per service (local):

- auth-service — http://localhost:8081/swagger-ui.html
- cv-service — http://localhost:8082/swagger-ui.html
- job-service — http://localhost:8083/swagger-ui.html
- roadmap-service — http://localhost:8085/swagger-ui.html

In production, services sit behind the gateway and are not individually exposed.

---

## Containerization

Every service and the frontend is containerized with **Docker**.

- **Multi-stage Dockerfiles** — build the JAR (or the Vite bundle) in one stage, run on a slim runtime image in the next, keeping images small.
- **Frontend** is served by **nginx**, which also proxies `/api` to the gateway and provides SPA fallback routing.
- **`docker-compose.yml`** spins up the full stack locally (services, gateway, discovery, config, admin, Redis, and the monitoring stack) on a shared Docker network with health checks.

```bash
cp .env.example .env   # fill in DB URLs, JWT secret, API keys
docker compose up --build
# Frontend → http://localhost:5173 (Vite dev) or the compose-mapped port
```

---

## Orchestration

The production workloads run on **Kubernetes (Oracle OKE)**, managed with **Kustomize**.

- **`k8s/base/`** — Deployments, Services and ConfigMaps for every component (business services, infrastructure, and the observability stack), all in the `bloom` namespace.
- **`k8s/overlays/`** — environment-specific overlays (`local`, `prod`).
- Centralized configuration is mounted into the Config Server via a `configMapGenerator` built from `config-repo/`.
- The frontend is exposed through a **LoadBalancer** service backed by the OCI load balancer.

---

## Infrastructure as Code

The cloud infrastructure is fully provisioned with **Terraform** (`terraform/oci/`) on **Oracle Cloud Infrastructure**.

**Network (`network.tf`)** — a dedicated **VCN** (`10.0.0.0/16`) with:
- a **public subnet** for the Kubernetes API endpoint (`10.0.0.0/28`),
- a **public subnet** for the load balancer (`10.0.1.0/24`),
- a **private subnet** for the worker nodes (`10.0.10.0/24`, no public IPs),
- an **Internet Gateway** (public traffic), a **NAT Gateway** (private outbound) and a **Service Gateway** (Oracle services),
- security lists opening only the required ports (80/443 public, 6443 for the K8s API, internal traffic within the VCN).

**Cluster (`oke.tf`)** — an OKE **BASIC_CLUSTER** with a managed node pool on flexible shapes (ARM/AMD), pods CIDR `10.244.0.0/16` and services CIDR `10.96.0.0/16`.

```bash
cd terraform/oci
terraform init
terraform apply
```

---

## CI/CD

Automated with **GitHub Actions** — one CI and one CD pipeline per service (`.github/workflows/`).

- **CI** (`ci-*.yaml`) runs on pull requests, in levels: **install → lint/compile → test → build**. The build fails if tests or coverage thresholds fail.
- **CD** (`cd-*.yaml`) runs on merge to `main`: builds a Docker image, pushes it to **Oracle Container Registry (OCIR)**, and deploys to **OKE** via `kubectl set image` + rollout. A reusable workflow (`cd-reusable.yaml`) is shared across services.
- Path filters ensure only the affected service is rebuilt on a change.

**Tools:** GitHub Actions, Docker Buildx, OCIR, `kubectl` / OKE.

---

## Observability

A full observability stack runs alongside the services (locally and in the cluster):

| Tool | Port (local) | Purpose |
|---|---|---|
| **Prometheus** | 9090 | Scrapes metrics from each service via Spring Boot Actuator |
| **Grafana** | 3001 | Dashboards for the gateway and platform overview |
| **Loki + Promtail** | 3100 | Aggregates and queries logs from all containers |
| **Zipkin** | 9411 | Distributed tracing across services |
| **Spring Boot Admin** | 8090 | Live health view of every registered service |

For example, **Grafana** visualizes request rates, latencies and JVM metrics; **Zipkin** lets you follow a single request as it travels gateway → service → service.

> In production these dashboards are **internal only** (`ClusterIP`) — only the frontend is publicly exposed. Access them in the cluster with `kubectl port-forward` (e.g. `kubectl port-forward svc/grafana 3001:3000 -n bloom`).

---

## Deployment

**Live app (Oracle Cloud):** http://140.238.123.230

The platform is deployed on **Oracle Kubernetes Engine (OKE)** inside a Terraform-provisioned VCN:

- **Only the frontend is exposed publicly** — it is the single `LoadBalancer` Service, fronted by the **OCI Load Balancer** (public LB subnet) and reachable at the public IP.
- **All other components are internal** (`ClusterIP`): the API Gateway, discovery (Eureka), config-server, admin-server and the full observability stack (Grafana, Prometheus, Loki, Zipkin) are **not** publicly exposed. The frontend's nginx proxies `/api` to the gateway internally; internal dashboards are reached via `kubectl port-forward`.
- **Worker nodes** run in a **private subnet** with no public IPs; they reach the internet only through the **NAT Gateway**, which keeps the internal services unreachable from outside the cluster.
- The **Kubernetes API** is exposed on its own public subnet, restricted by security lists.
- Internal service-to-service traffic stays inside the VCN (`10.0.0.0/16`); only ports 80/443 are open to the public.
- Databases are **managed PostgreSQL** instances (Supabase / Azure) reached over TLS, and **Redis** provides the job cache.

| Environment | Frontend URL |
|---|---|
| Local (Vite dev) | http://localhost:5173 |
| Production (OKE) | http://140.238.123.230 |

---

## Conclusion

Bloom delivers a complete microservices platform that connects what a student knows to what the job market wants. It covers the full lifecycle: requirements, UML design, secured REST APIs, a relational database per service with migrations, tested services with coverage gates, automated CI/CD, infrastructure as code, container orchestration, and a monitored production deployment on the cloud. The architecture is modular by design — new services can be added without touching the existing ones.

---

## Accessing Deployed Services (port-forward)

In production only the frontend is publicly exposed. The internal dashboards and the deployed services' API docs are reached through `kubectl port-forward` — the URL stays `localhost` but the data comes from the **production** cluster. Each command runs in its own terminal and stays open while in use (`Ctrl+C` to stop).

```bash
# Eureka (discovery)
kubectl port-forward svc/discovery-server 8761:8761 -n bloom
#  -> http://localhost:8761

# Spring Boot Admin
kubectl port-forward svc/admin-server 8090:8090 -n bloom
#  -> http://localhost:8090

# Grafana   (the service listens on 3000 inside the cluster)
kubectl port-forward svc/grafana 3001:3000 -n bloom
#  -> http://localhost:3001

# Prometheus
kubectl port-forward svc/prometheus 9090:9090 -n bloom
#  -> http://localhost:9090

# Zipkin
kubectl port-forward svc/zipkin 9411:9411 -n bloom
#  -> http://localhost:9411

# API Gateway (to hit the production API directly)
kubectl port-forward svc/api-gateway 8080:8080 -n bloom
#  -> http://localhost:8080
```

### Swagger / OpenAPI of the deployed services

```bash
# auth-service
kubectl port-forward svc/auth-service 8081:8081 -n bloom
#  -> http://localhost:8081/swagger-ui.html

# cv-service
kubectl port-forward svc/cv-service 8082:8082 -n bloom
#  -> http://localhost:8082/swagger-ui.html

# job-service
kubectl port-forward svc/job-service 8083:8083 -n bloom
#  -> http://localhost:8083/swagger-ui.html

# roadmap-service
kubectl port-forward svc/roadmap-service 8085:8085 -n bloom
#  -> http://localhost:8085/swagger-ui.html
```

---

> *"Don't just apply for jobs. Know exactly what you need to get there."*
