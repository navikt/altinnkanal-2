FROM openjdk:8-jre-alpine
ADD target/altinnkanal-less-enterprise-edition-1.0-SNAPSHOT.jar /app/
WORKDIR /app
EXPOSE 8080
CMD ["java","-jar","altinnkanal-less-enterprise-edition-1.0-SNAPSHOT.jar"]