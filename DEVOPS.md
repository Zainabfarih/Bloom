# Bloom — Guide DevOps (Conteneurisation & Orchestration)

Ce document explique la partie **DevOps** du projet Bloom : comment les
microservices sont **conteneurisés** (Docker) et **orchestrés** (Docker Compose)
pour pouvoir lancer toute la plateforme avec une seule commande.

> Périmètre couvert ici : infrastructure globale (config-server, discovery-server,
> api-gateway, admin-server), **job-service** et **cv-service**.
> Les services `auth-service` et `roadmap-service` (binôme) sont prévus via un
> profil Docker Compose dédié (`partner`).

---

## 1. Les 3 piliers DevOps de ce projet

| Pilier | Outil | Rôle |
|---|---|---|
| **Conteneurisation** | Docker (`Dockerfile`) | Empaqueter chaque service avec son runtime Java dans une image isolée et reproductible |
| **Orchestration** | Docker Compose (`docker-compose.yml`) | Démarrer/arrêter tous les conteneurs ensemble, gérer le réseau, l'ordre de démarrage et les dépendances |
| **CI** | GitHub Actions (`.github/workflows/`) | Compiler, tester et builder automatiquement à chaque Pull Request |

---

## 2. Conteneurisation — les Dockerfiles

Chaque service a un `Dockerfile` **multi-stage** (deux étapes) :

```
Étape 1 (build)    : image Maven + JDK 25  ->  compile le code et produit le .jar
Étape 2 (runtime)  : image JRE 25 Alpine    ->  ne garde que le .jar (image légère)
```

**Pourquoi multi-stage ?**
- L'image finale ne contient **pas** Maven ni le code source, seulement le JRE + le jar → image plus petite et plus sûre.
- Le cache Docker sur `pom.xml` évite de re-télécharger les dépendances à chaque build.

**Bonnes pratiques appliquées :**
- Utilisateur **non-root** (`bloom`) dans le conteneur (sécurité).
- `MaxRAMPercentage=75.0` : la JVM respecte la mémoire allouée au conteneur.
- `.dockerignore` : exclut `target/`, `.git/`, `.env`… du contexte de build.

Fichiers concernés :
```
services/job-service/Dockerfile
services/cv-service/Dockerfile
infrastructure/config-server/Dockerfile
infrastructure/discovery-server/Dockerfile
infrastructure/api-gateway/Dockerfile
infrastructure/admin-server/Dockerfile
```

---

## 3. Orchestration — Docker Compose

Le fichier `docker-compose.yml` à la racine décrit **toute la plateforme**.

### Schéma de démarrage (ordre géré par les `healthcheck`)

```
  postgres (5432)   redis (6379)
        │                │
        └──────┬─────────┘
               ▼
   discovery-server (8761)  ──►  config-server (8888)
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                          ▼
      job-service (8083)        cv-service (8082)         (auth/roadmap*)
            └─────────────┬───────────┘
                          ▼
                 api-gateway (8080)   +   admin-server (8090)
```
`*` profil `partner` (binôme), non démarré par défaut.

### Ports exposés

| Service | URL locale |
|---|---|
| API Gateway (point d'entrée) | http://localhost:8080 |
| Eureka (annuaire) | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| Admin Server (monitoring) | http://localhost:8090 |
| job-service | http://localhost:8083 |
| cv-service | http://localhost:8082 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

### Points techniques importants

- **Réseau** : tous les conteneurs sont sur le réseau `bloom-net` et se joignent par leur **nom de service** (ex. `http://config-server:8888`, `jdbc:postgresql://postgres:5432/...`). Le `localhost` du code (utile en dev local) est **surchargé** par des variables d'environnement (`SPRING_CONFIG_IMPORT`, `EUREKA_URI`) injectées par Compose — **le code source n'est pas modifié**.
- **Config Server en profil `native`** : il lit les fichiers du dossier `config-repo/` monté en volume (lecture seule). Pas besoin de token GitHub, fonctionne hors-ligne.
- **Bases par service** : le script `infrastructure/docker/postgres/init.sql` crée `bloom_jobs` et `bloom_cv` au premier démarrage (principe *Database-per-service*).
- **Volumes** : `postgres-data` et `redis-data` conservent les données entre les redémarrages.

---

## 4. Démarrage rapide

### Prérequis
- Docker Desktop installé et **démarré**.
- Un fichier `.env` à la racine (voir `.env.example`).

```powershell
# 1) (si besoin) créer le .env à partir du modèle
Copy-Item .env.example .env

# 2) construire et démarrer toute la plateforme
docker compose up -d --build

# 3) suivre les logs d'un service
docker compose logs -f job-service

# 4) voir l'état des conteneurs
docker compose ps

# 5) tout arrêter (les données sont conservées)
docker compose down

# 6) tout arrêter ET supprimer les bases (reset complet)
docker compose down -v


## 5. Vérifier que tout tourne

```powershell
# Santé de la gateway
curl http://localhost:8080/actuator/health

# Services enregistrés dans Eureka (ouvrir dans le navigateur)
start http://localhost:8761

# Dashboard de monitoring
start http://localhost:8090
