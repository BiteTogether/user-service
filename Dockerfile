# syntax=docker/dockerfile:1.4

# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

ARG COMMON_VERSION=0.0.4-SNAPSHOT
COPY libs/common-service-${COMMON_VERSION}.jar /tmp/common-service.jar

RUN mvn -B org.apache.maven.plugins:maven-install-plugin:3.1.0:install-file \
    -Dfile=/tmp/common-service.jar \
    -DgroupId=io.github.bitetogether \
    -DartifactId=common-service \
    -Dversion=${COMMON_VERSION} \
    -Dpackaging=jar

# ⚠️ Không dùng settings.xml & không gọi GitHub registry nữa
# => Tối ưu cache dependency (sẽ dùng local maven repo)
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
