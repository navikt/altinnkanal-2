FROM openjdk:8-jre-alpine
ADD target/altinnkanal.jar /app/
WORKDIR /app
EXPOSE 8080
CMD ["java","-jar","altinnkanal.jar"]