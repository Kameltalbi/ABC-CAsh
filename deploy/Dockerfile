FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY server/build/libs/server-1.0.0.jar /app/abc-cash-api.jar

EXPOSE 8081

ENV PORT=8081
ENV HOST=0.0.0.0

ENTRYPOINT ["java", "-jar", "/app/abc-cash-api.jar"]
