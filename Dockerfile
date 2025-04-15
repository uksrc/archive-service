FROM eclipse-temurin:17-jdk-alpine

RUN apk add --no-cache gettext

WORKDIR /app
COPY build/quarkus-app/lib/ /app/lib/
COPY build/quarkus-app/*.jar /app/
COPY build/quarkus-app/app/ /app/app/
COPY build/quarkus-app/quarkus/ /app/quarkus/

COPY ./src/main/resources/templates/ /
COPY src/main/config/entrypoint.sh /entrypoint.sh

#CMD ["java", "-jar", "quarkus-run.jar"]
ENTRYPOINT ["/entrypoint.sh"]