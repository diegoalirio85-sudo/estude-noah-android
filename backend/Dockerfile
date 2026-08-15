FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /workspace/target/estude-noah-backend-*.jar app.jar
ENV PORT=8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
USER app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
