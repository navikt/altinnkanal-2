FROM ghcr.io/navikt/baseimages/temurin:11

ENV JAVA_OPTS="-XshowSettings:vm -Dlogback.configurationFile=logback-remote.xml -XX:MaxRAMPercentage=75"
ENV APPLICATION_PROFILE="remote"
COPY altinnkanal/build/libs/*.jar app.jar
