FROM navikt/java:8

COPY altinnkanal/build/install/altinnkanal/bin/altinnkanal bin/app
COPY altinnkanal/build/install/altinnkanal/lib lib/
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
