# ---- build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the descriptor first so dependency resolution is cached as its own layer.
COPY pom.xml .
RUN mvn -B -e dependency:go-offline

COPY src ./src
RUN mvn -B -e clean package -DskipTests

# ---- runtime stage ----------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/target/storefront-*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
