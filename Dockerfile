# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests -q

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/butex-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
