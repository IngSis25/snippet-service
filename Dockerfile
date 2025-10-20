FROM gradle:8.5.0-jdk17 AS build
COPY  . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble
FROM eclipse-temurin:17-jre
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/snippet-service.jar
ENTRYPOINT ["java", "-jar", "/app/snippet-service.jar"]