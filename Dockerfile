FROM gradle:8.14-jdk17 AS builder

WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
COPY src ./src

RUN gradle clean bootJar --no-daemon -x test && \
    JAR_FILE="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" && \
    cp "$JAR_FILE" /tmp/app.jar

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN addgroup --system evready && adduser --system --ingroup evready evready

COPY --from=builder /tmp/app.jar /app/app.jar

RUN mkdir -p /app/logs && chown -R evready:evready /app

USER evready

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]