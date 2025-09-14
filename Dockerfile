FROM openjdk:21

WORKDIR /app

COPY src/main/resources/public ./public

COPY target/Taller1AREP-1.0-SNAPSHOT-jar-with-dependencies.jar .

CMD ["java", "-jar", "Taller1AREP-1.0-SNAPSHOT-jar-with-dependencies.jar", "8080"]