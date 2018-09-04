FROM navikt/java:8

COPY altinnkanal/build/install/* /app
CMD ["/app/bin/altinnkanal"]
