# Use lightweight OpenJDK image
FROM openjdk:17-slim

# Set working directory in container
WORKDIR /app

# Copy the built JAR into the container
ARG JAR_FILE=target/github-repository-scorer-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# Expose application port
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
