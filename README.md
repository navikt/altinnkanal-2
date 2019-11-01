# Altinnkanal 2

[![CircleCI](https://circleci.com/gh/navikt/altinnkanal-2.svg?style=svg)](https://circleci.com/gh/navikt/altinnkanal-2)

Altinnkanal-2 is an application written in Kotlin that exposes a webservice endpoint, and handles routing
of incoming messages from Altinn to internal systems by using Kafka.

## Technologies & Tools

* [Kotlin](https://kotlinlang.org)
* [Kafka](https://kafka.apache.org)
* [CXF](https://cxf.apache.org)
* [Jetty](https://eclipse.org/jetty)
* [Gradle](https://gradle.org)
* [Spek](http://spekframework.org)

## Getting started

### Compile, build and run tests
`./gradlew clean build`

### Generate startup scripts and distribution files:
`./gradlew installDist`

The files needed for distribution should then be available under 

`/altinnkanal/build/install/`

with startup scripts under:

`/altinnkanal/build/install/altinnkanal/bin/`

### Running locally

- Set up a local Kafka cluster using [this docker-compose project](https://github.com/navikt/navkafka-docker-compose).
- Start the application.
- A Webservice (as defined by the WSDL in [OnlineBatchReceiver.wsdl](altinnkanal/src/main/resources/OnlineBatchReceiver.wsdl)) 
should be available at: http://localhost:8080/webservices/OnlineBatchReceiverSoap

### Contact us
NAV employees may reach us on Slack in the channel #team_altinn_difi.
