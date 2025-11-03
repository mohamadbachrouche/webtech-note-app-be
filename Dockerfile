# Use a valid Gradle image tag that includes JDK 25
# 'noble' is the Ubuntu 24.04-based image, which is current for JDK 25
FROM gradle:jdk25-noble AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

# Use a valid Temurin image for JDK 25
FROM eclipse-temurin:25-jdk-noble
# Your original Dockerfile had the correct JAR name
COPY --from=build /home/gradle/src/build/libs/webtech-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]