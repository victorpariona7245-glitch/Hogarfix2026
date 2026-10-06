# 1. Etapa de Construcción (Build Stage)
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

COPY src /app/src

RUN mvn clean package -DskipTests

# 2. Etapa de Ejecución (Run Stage)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/hogarfix-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]