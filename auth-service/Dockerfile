FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY search-service/pom.xml search-service/pom.xml
RUN mvn -pl auth-service -am dependency:go-offline
COPY auth-service auth-service
RUN mvn -pl auth-service -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
