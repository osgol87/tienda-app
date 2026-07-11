FROM docker.io/maven:3.9.11-eclipse-temurin-21-alpine AS build
COPY . .
RUN mvn clean package -DskipTests -B

FROM docker.io/eclipse-temurin:21-jre-alpine-3.22
COPY --from=build /target/userservice-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
