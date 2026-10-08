# Builds any one module: docker build --build-arg SERVICE=product-service .
FROM eclipse-temurin:17-jdk AS build
ARG SERVICE
WORKDIR /app
COPY . .
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -pl ${SERVICE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG SERVICE
COPY --from=build /app/${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
