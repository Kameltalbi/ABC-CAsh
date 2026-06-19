# Build stage — Gradle needs glibc (Debian), not Alpine musl
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /build

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle gradle/
COPY server/build.gradle.kts server/build.gradle.kts
COPY server/src server/src

RUN chmod +x gradlew && ./gradlew :server:jar --no-daemon -q

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /build/server/build/libs/server-1.0.0.jar /app/abc-cash-api.jar

EXPOSE 8081

ENV PORT=8081
ENV HOST=0.0.0.0

ENTRYPOINT ["java", "-jar", "/app/abc-cash-api.jar"]
