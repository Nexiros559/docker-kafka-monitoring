FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. On copie TOUS les poms pour que le parent soit content
COPY pom.xml .
COPY monitoring-records/pom.xml monitoring-records/
COPY monitoring-producer/pom.xml monitoring-producer/
COPY monitoring-consumer/pom.xml monitoring-consumer/
COPY monitoring-api/pom.xml monitoring-api/

# 2. On télécharge les dépendances (cache)
# Note : on ignore les erreurs si un module manque, l'important c'est de charger les libs
RUN mvn dependency:go-offline -B -pl monitoring-api -am || true

# 3. On copie seulement le code source utile
COPY monitoring-records/src monitoring-records/src
COPY monitoring-api/src monitoring-api/src

# 4. On crée les dossiers manquants pour les autres modules (astuce pour feinter Maven)
RUN mkdir -p monitoring-producer/src/main/java && \
    mkdir -p monitoring-consumer/src/main/java

# 5. On compile
RUN mvn package -DskipTests -pl monitoring-api -am

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/monitoring-api/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]