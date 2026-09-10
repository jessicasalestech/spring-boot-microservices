# ---------------------------------------------------------------------------
# Build stage - resolves the Maven reactor and packages the executable jar
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy the reactor descriptors first so dependency layers are cached
COPY pom.xml ./
COPY product-service/pom.xml product-service/
COPY order-service/pom.xml order-service/
COPY api-gateway/pom.xml api-gateway/

# Copy the sources of this module and build it with its parent
COPY product-service/src product-service/src

RUN mvn -B -pl product-service -am -DskipTests package

# ---------------------------------------------------------------------------
# Runtime stage
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring

WORKDIR /app

COPY --from=build /workspace/product-service/target/product-service-*.jar app.jar

RUN chown -R spring:spring /app

USER spring:spring

EXPOSE 8081

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
    CMD curl -fsS http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
