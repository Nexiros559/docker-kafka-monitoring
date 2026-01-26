FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copie de TOUS les poms
COPY pom.xml .
COPY monitoring-records/pom.xml monitoring-records/
COPY monitoring-producer/pom.xml monitoring-producer/
COPY monitoring-consumer/pom.xml monitoring-consumer/
COPY monitoring-api/pom.xml monitoring-api/

# 2. Dépendances
RUN mvn dependency:go-offline -B -pl monitoring-producer -am || true

# 3. Sources utiles
COPY monitoring-records/src monitoring-records/src
COPY monitoring-producer/src monitoring-producer/src

# 4. Feinte Maven
RUN mkdir -p monitoring-consumer/src/main/java && \
    mkdir -p monitoring-api/src/main/java

# 5. Compile
RUN mvn package -DskipTests -pl monitoring-producer -am

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/monitoring-producer/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]