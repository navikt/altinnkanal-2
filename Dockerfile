FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-11

ENV JAVA_OPTS="-XshowSettings:vm -Dlogback.configurationFile=logback-remote.xml -XX:MaxRAMPercentage=75"
ENV APPLICATION_PROFILE="remote"
COPY altinnkanal/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]