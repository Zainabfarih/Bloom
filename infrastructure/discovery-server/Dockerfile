# =====================================================================
#  discovery-server (Eureka) — image Docker multi-stage
# =====================================================================

# ---------- Etape 1 : BUILD ----------
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
ENV MAVEN_OPTS="-Daether.connector.http.retryHandler.count=10 -Daether.connector.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.http.retryHandler.count=10"
COPY pom.xml .
COPY src ./src
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
RUN apk add --no-cache curl \
 && addgroup -S bloom && adduser -S bloom -G bloom
COPY --from=build /app/target/*.jar app.jar
USER bloom
EXPOSE 8761
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
