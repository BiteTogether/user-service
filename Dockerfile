# syntax=docker/dockerfile:1.4

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy template settings.xml trước
COPY maven/settings.template.xml /root/.m2/settings.template.xml

# Tạo file settings.xml từ template bằng secrets
RUN --mount=type=secret,id=github_token \
    --mount=type=secret,id=github_username \
    TOKEN=$(cat /run/secrets/github_token) && \
    USERNAME=$(cat /run/secrets/github_username) && \
    sed -e "s|{{GITHUB_TOKEN}}|${TOKEN}|g" \
        -e "s|{{GITHUB_USERNAME}}|${USERNAME}|g" \
        /root/.m2/settings.template.xml > /root/.m2/settings.xml

# Copy toàn bộ project (bao gồm pom.xml, src/, etc.)
COPY . .

# (Tuỳ chọn) Tối ưu cache bằng cách chạy dependency trước
RUN mvn dependency:go-offline -B

# Build ứng dụng
RUN mvn clean package -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown -R spring:spring /app
USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
