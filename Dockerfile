# Build stage
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=80

EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]