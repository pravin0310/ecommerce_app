FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./pom.xml

COPY discovery-server/pom.xml ./discovery-server/pom.xml
COPY config-server/pom.xml ./config-server/pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
COPY product-service/pom.xml ./product-service/pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml
COPY inventory-service/pom.xml ./inventory-service/pom.xml
COPY notification-service/pom.xml ./notification-service/pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml

RUN mvn install -N -B
RUN mvn dependency:go-offline -B -pl discovery-server || true

COPY discovery-server/src ./discovery-server/src

RUN mvn clean package -DskipTests -B -pl discovery-server

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java","-Duser.timezone=Asia/Kolkata","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-jar","app.jar"]