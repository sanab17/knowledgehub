# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy src and build
COPY src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for security compliance in production
RUN addgroup -S spring && adduser -S spring -G spring

# Create uploads directory and grant ownership to the non-root user
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads /app

USER spring:spring

# Copy built artifact from build stage
COPY --from=build --chown=spring:spring /app/target/knowledgehub-*.jar app.jar

# Configure JVM flags & Application ports
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
