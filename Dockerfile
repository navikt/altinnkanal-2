FROM navikt/java:8
ENV JAVA_OPTS="-Dspring.profiles.active=remote"
COPY ./*.jks /
