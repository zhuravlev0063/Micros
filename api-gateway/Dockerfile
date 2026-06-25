FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY search-service/pom.xml search-service/pom.xml
RUN mvn -pl api-gateway -am dependency:go-offline
COPY api-gateway api-gateway
RUN mvn -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway-0.1.0.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
