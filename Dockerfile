# ============================================================
#  Dockerfile for the Stack ICT Academy LMS (Spring Boot)
#  Works on container hosts: Render, Google Cloud Run, Railway, Fly.io.
#  NOTE: Vercel / Netlify / Supabase cannot run this — they don't run
#  containers or a JVM. Use one of the hosts above.
# ============================================================

# ---- Stage 1: build the jar with Maven + JDK 17 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Cache dependencies first (faster rebuilds)
COPY pom.xml .
RUN mvn -q -e dependency:go-offline
# Then build
COPY src ./src
RUN mvn -q -DskipTests package

# ---- Stage 2: small runtime image with just a JRE ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/lms-0.0.1.jar app.jar

# Hosts inject the port to listen on via $PORT (Cloud Run/Render default 8080)
ENV PORT=8080
EXPOSE 8080

# Bind to the host-provided port
CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
