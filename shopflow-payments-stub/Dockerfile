FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Parent POM first (provides Spring Boot version management)
COPY pom.xml pom.xml

# Only the stub module — orders module is not needed
COPY shopflow-payments-stub/pom.xml shopflow-payments-stub/pom.xml
COPY shopflow-payments-stub/src shopflow-payments-stub/src

RUN mvn -f shopflow-payments-stub/pom.xml package -DskipTests --no-transfer-progress

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/shopflow-payments-stub/target/shopflow-payments-stub-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
