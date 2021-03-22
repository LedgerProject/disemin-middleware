FROM openjdk:8-jdk-alpine
ARG JAR_FILE=target/agroxm-middleware.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]