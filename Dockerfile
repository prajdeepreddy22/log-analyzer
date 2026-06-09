FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app

WORKDIR /app

COPY --from=build /workspace/target/log-analyzer-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads \
    && chown -R app:app /app

USER app

ENV PORT=8080
ENV STORAGE_BASE_PATH=/app/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
