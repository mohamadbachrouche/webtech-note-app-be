# Build stage
FROM eclipse-temurin:25-jdk-jammy AS build

# Install tools and Gradle 9.1.0
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y curl unzip && rm -rf /var/lib/apt/lists/*
ENV GRADLE_VERSION=9.1.0
RUN curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o gradle.zip \
    && unzip -d /opt/gradle gradle.zip \
    && rm gradle.zip
ENV PATH="/opt/gradle/gradle-${GRADLE_VERSION}/bin:${PATH}"

WORKDIR /workspace

# Copy build files first to leverage Docker layer caching
COPY build.gradle settings.gradle ./

# Copy the application source
COPY src src

# Build the Spring Boot fat jar (skip tests for faster image builds)
RUN gradle --no-daemon clean bootJar -x test

# Runtime image
FROM eclipse-temurin:25-jdk-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /workspace/build/libs/webtech-0.0.1-SNAPSHOT.jar app.jar

# Expose the default port
EXPOSE 8080

# Run the app, allowing Render to pass in JAVA_OPTS
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]