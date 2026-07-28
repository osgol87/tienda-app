# Use Maven to build the application, then use a minimal JRE image to run it
FROM docker.io/maven:3.9.11-eclipse-temurin-21-alpine AS build
# Copy source code and build
COPY . .
RUN mvn clean package -DskipTests -B

FROM docker.io/eclipse-temurin:21-jre-alpine-3.22
# Using a wildcard is more robust to version changes
COPY --from=build /target/productserviceelastic-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
