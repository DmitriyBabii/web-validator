FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY ./pom.xml .
COPY ./src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /application
COPY --from=build /app/target/*.jar app.jar

WORKDIR /application
CMD ["java", "-jar", "app.jar"]

EXPOSE 8080
