FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -Dmaven.test.skip=true dependency:go-offline

COPY src src
RUN ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build --chown=spring:spring /workspace/target/backend-*.jar app.jar

USER spring:spring

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]
