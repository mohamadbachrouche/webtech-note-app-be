# Build stage
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace

# Copy Gradle wrapper and build files first to leverage Docker layer caching
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Run a simple Gradle command to download the wrapper
RUN chmod +x gradlew
RUN ./gradlew --no-daemon -v || true

# Copy the application source
COPY src src

# Build the Spring Boot fat jar (using the artifact name we confirmed earlier)
RUN ./gradlew --no-daemon clean build bootJar -x test

# Runtime image
FROM eclipse-temurin:25-jdk-jammy
WORKDIR /app

# Use wildcard to be safe, but it will copy your webtech-0.0.1-SNAPSHOT.jar
COPY --from=build /workspace/build/libs/*.jar app.jar

# Expose the default port
EXPOSE 8080

# Run the app, allowing Render to pass in JAVA_OPTS
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]