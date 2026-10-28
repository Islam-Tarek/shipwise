# Stage 1: build
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests -pl gateway -am dependency:go-offline
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests -pl gateway -am package

# Stage 2: runtime
FROM eclipse-temurin:25-jre AS run
WORKDIR /app
COPY --from=build /build/gateway/target/*.jar app.jar
USER 1000
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]