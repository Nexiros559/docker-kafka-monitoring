FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY monitoring-records monitoring-records
COPY monitoring-producer monitoring-producer
COPY monitoring-consumer monitoring-consumer
RUN mvn clean package -pl monitoring-records,monitoring-producer -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/monitoring-producer/target/monitoring-producer-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
