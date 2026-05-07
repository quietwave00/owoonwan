FROM node:20-bookworm-slim AS client-build
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend ./
RUN npm run build

FROM eclipse-temurin:17-jdk-jammy AS server-build
WORKDIR /app

COPY gradle gradle
COPY gradlew gradlew.bat build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies

COPY src src
RUN rm -rf src/main/resources/static && mkdir -p src/main/resources/static
COPY --from=client-build /app/frontend/dist src/main/resources/static

RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=server-build /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-XX:InitialRAMPercentage=20", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
