FROM navikt/java:8
COPY target/*.jar /app/app.jar
ENV JAVA_OPTS="-Dspring.profiles.active=local -Djavax.net.ssl.trustStore=/var/run/secrets/naisd.io/app_truststore_keystore -Djavax.net.ssl.trustStorePassword=$APP_TRUSTSTORE_PASSWORD"

