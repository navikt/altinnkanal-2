FROM openjdk:8-jre-alpine
ADD target/altinnkanal-spring-1.0-SNAPSHOT.jar /app/
WORKDIR /app
EXPOSE 8080
CMD ["java","-jar","altinnkanal-spring-1.0-SNAPSHOT.jar"]