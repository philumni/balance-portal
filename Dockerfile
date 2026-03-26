# ============================================================
# Balance Portal — Dockerfile
# ============================================================
# Multi-stage build:
#   Stage 1 (builder) — Maven compiles the WAR
#   Stage 2 (runtime) — Tomcat 10 serves it
#
# Multi-stage means the final image only contains Tomcat + the WAR.
# The JDK, Maven, and all build tools are left behind in stage 1.
# This keeps the production image small and reduces attack surface.
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml first — Docker caches this layer.
# If only source files change (not pom.xml), Maven dependencies
# are NOT re-downloaded on the next build. Big time saver.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Now copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM tomcat:10.1-jdk17

# Remove the default Tomcat webapps (ROOT, examples, docs)
# so port 8080 isn't cluttered with demo apps
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the WAR from the builder stage
COPY --from=builder /build/target/balance-portal-db.war \
     /usr/local/tomcat/webapps/ROOT.war

# Tomcat auto-deploys any .war in the webapps directory on startup.
# No extra configuration needed.

# Expose the default Tomcat port
EXPOSE 8080

# Tomcat's startup script
CMD ["catalina.sh", "run"]
