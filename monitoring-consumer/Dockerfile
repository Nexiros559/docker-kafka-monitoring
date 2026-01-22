FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copie le JAR généré par Maven
COPY target/monitoring-consumer-1.0-SNAPSHOT.jar app.jar

# Lance l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
