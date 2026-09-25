# ─── Stage 1: Build Application ───
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix Windows CRLF line endings on mvnw and give execution rights
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Pre-fetch dependencies for better build caching
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ─── Stage 2: Runtime Image ───
FROM eclipse-temurin:25-jre
WORKDIR /app

# Ensure Docker container runs in prod profile by default
ENV SPRING_PROFILES_ACTIVE=prod

# Create directory for file uploads
RUN mkdir -p /app/uploads/cars

# Copy the executable JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
