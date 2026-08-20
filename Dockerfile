FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copie du JAR généré
COPY target/*.jar app.jar

ENV TZ=Europe/Paris

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Europe/Paris", "-jar", "app.jar"]