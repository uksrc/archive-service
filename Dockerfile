FROM eclipse-temurin:17-jdk-alpine


WORKDIR /app
COPY build/quarkus-app/lib/ /app/lib/
COPY build/quarkus-app/*.jar /app/
COPY build/quarkus-app/app/ /app/app/
COPY build/quarkus-app/quarkus/ /app/quarkus/

CMD ["java", "-jar", "quarkus-run.jar"]