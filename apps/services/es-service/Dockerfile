FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY apps/services/es-service/pom.xml /app/apps/services/es-service/pom.xml
COPY apps/services/es-service/src /app/apps/services/es-service/src

RUN mvn -pl apps/services/es-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/apps/services/es-service/target/es-service-0.1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8086
ENTRYPOINT ["java","-jar","/app/app.jar"]
