# =====================================================================
#  job-service — image Docker (multi-stage)
#  Etape 1 (build)   : on compile le projet avec Maven + JDK 25
#  Etape 2 (runtime) : on ne garde que le JRE + le jar => image legere
# =====================================================================

# ---------- Etape 1 : BUILD ----------
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Resilience reseau : le resolver Maven re-essaie les telechargements coupes
ENV MAVEN_OPTS="-Daether.connector.http.retryHandler.count=10 -Daether.connector.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.http.retryHandler.count=10"

# On copie le pom puis le code source
COPY pom.xml .
COPY src ./src

# Build avec boucle de retry + cache .m2 persistant (BuildKit) :
# - le cache /root/.m2 survit entre les builds ET entre les tentatives,
#   donc on ne re-telecharge jamais deux fois le meme jar (robuste sur
#   reseau instable). Tests ignores ici (ils tournent en CI).
RUN --mount=type=cache,target=/root/.m2 \
    ok=""; \
    for i in 1 2 3 4 5; do \
      mvn -B -ntp clean package -DskipTests && ok=1 && break; \
      echo ">> Tentative $i echouee, nouvel essai dans 15s..."; \
      sleep 15; \
    done; \
    [ "$ok" = "1" ]

# ---------- Etape 2 : RUNTIME ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# curl pour le HEALTHCHECK + utilisateur non-root (securite)
RUN apk add --no-cache curl \
 && addgroup -S bloom && adduser -S bloom -G bloom

COPY --from=build /app/target/*.jar app.jar

USER bloom
EXPOSE 8083

# MaxRAMPercentage : la JVM respecte la RAM allouee au conteneur
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
