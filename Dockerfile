FROM navikt/java:8
COPY altinnkanal/build/install/* /app
ENV SPRING_PROFILES_ACTIVE="remote"
ENTRYPOINT ["/app/bin/altinnkanal"]
