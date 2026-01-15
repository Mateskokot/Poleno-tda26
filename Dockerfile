 # ===============================
# BUILD STAGE
# ===============================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Nejprve jen pom.xml (lepší cache)
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Pak zdrojáky
COPY src ./src
RUN mvn -B -DskipTests package

# ===============================
# RUNTIME STAGE
# ===============================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Zkopíruje výsledný JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
