# Étape 1 : On part d'une image avec Java 21 (comme ton projet)
FROM eclipse-temurin:21-jre-alpine

# On définit le dossier de travail dans le conteneur
WORKDIR /app

# On copie le JAR compilé de ton PC vers le conteneur
# Attention : Le prof devra avoir fait 'mvn clean install' avant,
# OU tu utilises un Dockerfile "Multi-stage" (plus pro, voir ci-dessous).
COPY target/monitoring-api-1.0-SNAPSHOT.jar app.jar

# La commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]