# --- BUILDER ---
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

# On lance le package
RUN mvn clean package -DskipTests

# DEBUG : Affiche la liste des fichiers créés pour qu'on soit sûrs !
RUN ls -l target/

# --- RUNNER ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# ASTUCE : On ne copie que le fichier qui NE commence PAS par 'original'
# Cela force Docker à prendre le Fat Jar.
COPY --from=builder /app/target/*.jar ./temp_jars/
RUN find ./temp_jars/ -name "*.jar" ! -name "original-*.jar" -exec cp {} app.jar \;

CMD ["java", "-jar", "app.jar"]