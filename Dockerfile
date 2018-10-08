FROM navikt/java:11

COPY altinnkanal/build/install/altinnkanal/bin/altinnkanal bin/app
COPY altinnkanal/build/install/altinnkanal/lib lib/
ENV JAVA_OPTS="-XshowSettings:vm -Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"