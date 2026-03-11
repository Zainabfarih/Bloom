# Bloom — Smart Career & Learning Platform for Students

> Match your skills to real opportunities. Know what to learn next. Never get lost on your path.

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
Students upload their CV and the system automatically extracts their skills, technologies, experiences, and education. Extracted skills are displayed on the student's profile, which they can customize freely.

### 3. Job & Internship Matching *(requires CV upload)*
Matches the student's extracted skills against a database of job and internship offers, ranked by compatibility. Clearly shows which required skills the student already has vs. which are missing.

### 4. Tech Role Catalog
A browsable catalog of tech-related job roles, each pre-configured with:
- A list of required skills
- A structured learning roadmap with steps and resources
- A suggested learning order

Available roles include (non-exhaustive):
- Software Engineer
- Frontend / Backend / Fullstack Developer
- Data Analyst
- Data Scientist
- Machine Learning Engineer
- DevOps / Cloud Engineer
- Cybersecurity Analyst
- Mobile Developer
- UI/UX Designer

### 5. Skill Gap Analysis
For each job offer or selected role, the system identifies exactly which skills the student is lacking. The student can accept or reject suggested skills to develop. Accepted skills are added to their Personal Learning Plan.

### 6. Personalized Learning Plan
A central space where the student manages all the skills they've chosen to learn — whether picked manually, suggested by a role, or triggered by a job offer gap. Features include:
- Structured roadmap per skill with steps and resources
- Skill prioritization
- Progress tracking

### 7. CV Improvement Suggestions
AI-powered analysis of the CV's structure, formatting, and written content — providing actionable feedback on what to add, improve, or restructure.

### 8. Learning Reminders
Students can schedule reminders for their learning sessions, and the app sends notifications to keep them on track.

### 9. Student Profile
Displays verified skills, skills currently being learned, and progress on active roadmaps.

---

## Target Users

| User Type | Description |
|---|---|
| **Students** | University or bootcamp students looking to grow in tech or find internships/jobs |
| **Recent Graduates** | People entering the job market who want to identify and fill skill gaps |
| **Career Changers** | Individuals pivoting to a new technical domain |

---

## Technology Stack

| Layer | Technology |
|---|---|
| **Frontend** | React.js |
| **Backend** | Spring Boot |
| **Database** | PostgreSQL |
| **AI Layer** | LLM API |

---

## License

This project is developed for academic purposes.

---

> *"Don't just apply for jobs. Know exactly what you need to get there."*
