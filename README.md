# Bloom — Smart Career & Learning Platform for Students

> Match your skills to real opportunities. Know what to learn next. Never get lost on your path.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [The Problem We Solve](#the-problem-we-solve)
3. [Non-Functional Requirements](#non-functional-requirements)
4. [Actors & Roles](#actors--roles)
5. [Core Features](#core-features)
6. [Technology Stack](#technology-stack)
7. [Architecture — Microservices](#architecture--microservices)
8. [Version Control & API Documentation](#version-control--api-documentation)
9. [Dependencies](#dependencies)
10. [Target Users](#target-users)
11. [License](#license)

---

## Project Overview

**Bloom** is a web application designed to bridge the gap between a student's current skillset and the tech job market. Students can explore structured learning paths tied to specific tech roles, build a personalized learning plan, and optionally upload their CV to get matched with real job and internship offers. Whether they want to learn for the sake of learning or actively target a career goal, Bloom adapts to their needs.

---

## The Problem We Solve

Students often face two core challenges:

- **They don't know what to learn** to reach their career goals or grow in a specific tech domain
- **They don't know which jobs they qualify for** based on their current skills, or what's missing to get there

Bloom addresses both in a single, unified platform.

---

## Non-Functional Requirements

| Requirement | Details |
|---|---|
| **Performance** | Job offers cached for 24h to minimize external API calls · AI responses processed asynchronously |
| **Scalability** | Microservices architecture — each service scales independently based on load |
| **Availability** | Circuit Breaker (Resilience4j) isolates failures — one service down does not affect others |
| **Security** | BCrypt passwords · stateless JWT · OAuth2 · role-based access · no secrets in source code |
| **Maintainability** | DB-per-service isolation · centralized config · independent deployments via Docker |

---

## Actors & Roles

| Actor | Role |
|---|---|
| **Student** | Main user — uploads CV, explores roles, builds learning plan, tracks progress |
| **Admin** | Manages job cache, monitors services, oversees platform data |
| **AI System** | Extracts skills from CVs, analyzes CV quality, generates personalized roadmaps |
| **Google Jobs API** | External source of real job and internship listings |


---

## Core Features

### 1. Two Learning Modes

When a student joins Bloom, they choose how they want to use the platform:

#### Mode A — Explore & Learn (no job target required)
The student browses a catalog of **tech job roles** (e.g. Software Engineer, Data Analyst, DevOps Engineer, UX Designer, etc.) and selects one or more roles they're interested in. Each role displays:
- A curated list of required skills
- A structured learning roadmap per skill
- Recommended resources (courses, docs, tutorials)

The student can freely add any skill to their **Personal Learning Plan** without needing to link it to a job application.

#### Mode B — CV-Based Job Matching
The student uploads their CV. The system extracts their skills and matches them against real job and internship listings, ranked by compatibility. For each offer, the student can see:
- Which required skills they already have
- Which skills are missing (skill gap)
- Option to add missing skills to their Personal Learning Plan

---

### 2. CV Upload & Skill Extraction
Students upload their CV (PDF or DOCX) and the system automatically extracts their skills, technologies, experiences, and education using AI. Extracted skills are displayed on the student's profile, which they can customize freely.

### 3. Job & Internship Matching *(requires CV upload)*
Matches the student's extracted skills against real job and internship offers fetched from Google Jobs API, ranked by compatibility score. Clearly shows which required skills the student already has vs. which are missing.

### 4. Tech Role Catalog
A browsable catalog of tech-related job roles, each pre-configured with:
- A list of required skills
- A structured learning roadmap with steps and resources
- A suggested learning order

Available roles include (non-exhaustive): Software Engineer, Frontend / Backend / Fullstack Developer, Data Analyst, Data Scientist, Machine Learning Engineer, DevOps / Cloud Engineer, Cybersecurity Analyst, Mobile Developer, UI/UX Designer.

### 5. Skill Gap Analysis
For each job offer or selected role, the system identifies exactly which skills the student is lacking. The student can accept or reject suggested skills to develop. Accepted skills are added to their Personal Learning Plan.

### 6. Personalized Learning Plan
A central space where the student manages all the skills they've chosen to learn — whether picked manually, suggested by a role, or triggered by a job offer gap. Includes structured roadmap per skill with steps and resources, skill prioritization, and progress tracking.

### 7. CV Improvement Suggestions
AI-powered analysis of the CV's structure, formatting, and written content — providing actionable feedback on what to add, improve, or restructure, including a quality score.

### 8. Learning Reminders
Students can schedule reminders for their learning sessions, and the app sends email notifications to keep them on track.

### 9. Student Profile
Displays verified skills, skills currently being learned, and progress on active roadmaps.

---

## Technology Stack

| Layer | Technology |
|---|---|
| **Frontend** | React 18 (Vite) |
| **Backend** | Spring Boot 3 + Spring Cloud |
| **Database** | PostgreSQL 16 |
| **Security** | Spring Security + JWT + OAuth2 (Google / GitHub) |
| **AI Layer** | Claude API / OpenAI |
| **Job Listings** | Google Jobs API (SerpApi) |
| **DevOps** | Docker · Docker Compose · GitHub Actions |

---

## Architecture — Microservices

Bloom is built with a **microservices architecture** using Spring Cloud. Each service is independently deployable, has its own dedicated PostgreSQL database, and communicates through the API Gateway. No service accesses another service's database directly — only via REST calls.

### The 7 Microservices

| Service | Port | Database | Description |
|---|---|---|---|
| **api-gateway** | 8080 | — | Single entry point for the frontend — handles routing and JWT validation |
| **auth-service** | 8081 | bloom_auth | User registration, login, and JWT token management |
| **cv-service** | 8082 | bloom_cv | CV upload (PDF/DOCX), AI-powered skill extraction, and CV analysis |
| **job-service** | 8083 | bloom_jobs | Job offer fetching, student–job matching, and skill gap detection |
| **roadmap-service** | 8084 | bloom_roadmap | AI-generated personalized learning roadmaps with step-by-step progression |
| **notification-service** | 8085 | bloom_notif | Learning reminders and email notifications |
| **community-service** | 8086 | bloom_community | Thematic groups by skill, resource sharing, and blog posts |

> **MVP Priority:** Auth → CV → Job → Roadmap. Notification and Community are stretch goals.

---

---

## Version Control & API Documentation

The project uses **Git** with a mono-repo structure. Branches follow the `feature/<service>-<feature>` convention (e.g. `feature/auth-jwt`, `feature/cv-upload`), merged directly into `main` via pull requests. Feature branches are deleted after merge — cloning `main` gives the complete, stable project. Commits are linked to GitHub Issues for traceability.

Each microservice exposes its API documentation via **Swagger / OpenAPI**, accessible at `/swagger-ui.html` on each service's port during development.

## Dependencies

### Backend — Common to all services
| Dependency | Purpose |
|---|---|
| Spring Web | REST API controllers |
| Spring Data JPA | Database access via repositories |
| PostgreSQL Driver | PostgreSQL connection |
| Lombok | Boilerplate reduction |
| Spring Validation | Bean Validation (`@Valid`) |
| Spring Actuator | Health checks and monitoring |
| Eureka Discovery Client | Service registration with Eureka |
| Spring Cloud Config Client | Fetch config from Config Server |
| HikariCP | Connection pooling (included by default in Spring Boot) |

### Backend — Service-specific
| Service | Extra Dependencies |
|---|---|
| **auth-service** | Spring Security · jjwt-api 0.12.3 · Spring OAuth2 Client |
| **cv-service** | Apache PDFBox 3.0.1 · Apache POI 5.2.5 · Spring Cloud OpenFeign |
| **job-service** | Spring Cloud OpenFeign · Google Jobs API (SerpApi) |
| **roadmap-service** | Spring Cloud OpenFeign |
| **notification-service** | Spring Mail |
| **api-gateway** | Spring Cloud Gateway |
| **infrastructure** | Spring Cloud Config Server · Eureka Server |

### Frontend
| Dependency | Purpose |
|---|---|
| React 18 + Vite | SPA framework and build tool |
| Axios | HTTP client for API calls |
| React Router DOM | Client-side routing |
| Material UI (MUI) | UI component library |
| react-toastify | User notifications |
| react-dropzone | CV file upload UI |
| react-circular-progressbar | Learning progress display |


---

## Target Users

| User Type | Description |
|---|---|
| **Students** | University or bootcamp students looking to grow in tech or find internships/jobs |
| **Recent Graduates** | People entering the job market who want to identify and fill skill gaps |
| **Career Changers** | Individuals pivoting to a new technical domain |

---

## License

This project is developed for academic purposes — Génie Logiciel 2025.

---

> *"Don't just apply for jobs. Know exactly what you need to get there."*
