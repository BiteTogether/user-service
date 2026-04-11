# syntax=docker/dockerfile:1.4

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

ARG COMMON_VERSION=0.0.7-SNAPSHOT
COPY libs/common-service-${COMMON_VERSION}.jar /tmp/common-service.jar

RUN mvn -B org.apache.maven.plugins:maven-install-plugin:3.1.0:install-file \
    -Dfile=/tmp/common-service.jar \
    -DgroupId=io.github.bitetogether \
    -DartifactId=common-service \
    -Dversion=${COMMON_VERSION} \
    -Dpackaging=jar

RUN mvn dependency:go-offline -B

# Build ứng dụng
RUN mvn clean package -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

# Copy JAR file
COPY --from=builder /app/target/*.jar app.jar

# Copy SSL certificates for Kafka
COPY --from=builder /app/src/main/resources/client.keystore.p12 /app/certs/client.keystore.p12
COPY --from=builder /app/src/main/resources/client.truststore.jks /app/certs/client.truststore.jks

# Copy Firebase credentials
COPY --from=builder /app/src/main/resources/config/firebase-service-account.json /app/config/firebase-service-account.json

# Create directories and set permissions
RUN mkdir -p /app/config && \
    chown -R spring:spring /app && \
    chmod 600 /app/certs/client.keystore.p12 /app/certs/client.truststore.jks && \
    chmod 600 /app/config/firebase-service-account.json

USER spring

EXPOSE 8081

# Credentials are loaded from JAR's classpath by default
ENTRYPOINT ["java", "-jar", "app.jar"]
