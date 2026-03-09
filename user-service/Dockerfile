# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy ROOT parent pom
COPY pom.xml ./pom.xml

# Copy ALL service poms (parent pom needs all modules)
COPY discovery-server/pom.xml ./discovery-server/pom.xml
COPY config-server/pom.xml ./config-server/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY product-service/pom.xml ./product-service/pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml
COPY inventory-service/pom.xml ./inventory-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml

# Install parent pom into local Maven repo
RUN mvn install -N -B

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B -pl user-service || true

# Copy source
COPY user-service/src ./user-service/src

# Build jar
RUN mvn clean package -DskipTests -B -pl user-service

# Stage 2 — Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/user-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", \
  "-Duser.timezone=Asia/Kolkata", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]