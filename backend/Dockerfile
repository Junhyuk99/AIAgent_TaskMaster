# Pre-built JAR (no internet required)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 appgroup && adduser -u 1001 -G appgroup -D appuser

# Copy pre-built jar
COPY build/libs/*.jar app.jar

# Create upload directory
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
