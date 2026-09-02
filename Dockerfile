FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY indexer-core/pom.xml indexer-core/pom.xml
COPY indexer-mcp/pom.xml indexer-mcp/pom.xml
COPY indexer-api/pom.xml indexer-api/pom.xml
COPY indexer-core indexer-core
COPY indexer-mcp indexer-mcp
COPY indexer-api indexer-api
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/indexer-api/target/indexer-api-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
