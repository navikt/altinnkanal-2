FROM navikt/java:8
COPY *.jks ./
ENV JAVA_OPTS="-Dspring.profiles.active=local -Djavax.net.ssl.trustStore=preprod.truststore.jks -Djavax.net.ssl.trustStorePassword=password -Dcom.sun.net.ssl.checkRevocation=false"

