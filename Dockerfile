FROM openjdk:8-jre-alpine

ENV LC_ALL="no_NB.UTF-8"
ENV LANG="no_NB.UTF-8"
ENV TZ="Europe/Oslo"

WORKDIR /app

EXPOSE 8080

COPY altinnkanal/build/install/* /app
CMD ["/app/bin/altinnkanal"]
