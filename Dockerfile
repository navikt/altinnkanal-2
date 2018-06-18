FROM navikt/java:8

COPY altinnkanal/build/install/* /app
ENTRYPOINT ["/app/bin/altinnkanal"]
