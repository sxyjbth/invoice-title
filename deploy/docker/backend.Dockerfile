FROM m.daocloud.io/docker.io/library/maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY scripts/maven-settings.xml scripts/maven-settings.xml
RUN mvn --batch-mode -f backend/pom.xml \
    -s scripts/maven-settings.xml \
    dependency:go-offline

COPY backend/src backend/src
RUN mvn --batch-mode -f backend/pom.xml \
    -s scripts/maven-settings.xml \
    clean verify

FROM m.daocloud.io/docker.io/library/eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 invoice-title \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin invoice-title

WORKDIR /app
COPY --from=builder --chown=10001:10001 \
    /workspace/backend/target/invoice-title-service-0.1.0-SNAPSHOT.jar \
    /app/invoice-title-service.jar

USER 10001:10001
EXPOSE 28082

ENTRYPOINT ["java", "-Xms256m", "-Xmx768m", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/invoice-title-service.jar"]
