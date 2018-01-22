FROM navikt/java:8
COPY altinnkanal/target/*.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE="remote"
