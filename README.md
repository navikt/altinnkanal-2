# Altinnkanal 2

Repository for Altinnkanal 2. Application written in Kotlin to handle routing of messages from Altinn from a webservice
 endpoint to internal systems using Kafka. 

## Technologies & Tools

* Kotlin
* Kafka
* CXF
* Jetty
* Gradle

For deployment:
* Docker (tested on 17.03.1-ce)

## Notes on local development

### Build, test, run and deploy
Generally, we recommend just checking in code to the repository for packaging and deployment as our Jenkins CI server automatically handles this.

Nonetheless, notes on the process are available in the following sections.

#### Environment variables
The environment variables do not have to be set for running tests, however they must be set before running the application locally:

`SOAP_USERNAME=<username>` and `SOAP_PASSWORD=<password>`

#### SSL

Broker connection requires SSL. Connection to schema-registry also requires SSL.

This will cause missing certs exception in JVM.

Fix: Download the [NAV truststore](https://fasit.adeo.no/resources/3816117) and add these options to the VM:

```
-Djavax.net.ssl.trustStore=path\to\<truststore>.jts
-Djavax.net.ssl.trustStorePassword=<truststore password>
```

#### Build, run and test

Make sure to run to generate required code:

```./gradlew clean build```

#### Package distribution

Run this command to generate needed JARs and startup scripts:

```./gradlew clean installDist```

Create a Docker image using the included Dockerfile (see the notes below on Docker if you're on Windows):

```
docker build -f path\to\Dockerfile -t integrasjon/altinnkanal-2:<version>

docker tag integrasjon/altinnkanal-2:<version> repo.adeo.no:5443/integrasjon/altinnkanal-2:<version>

docker push repo.adeo.no:5443/integrasjon/altinnkanal-2:<version>
```

#### Deploy

Deployment to NAIS. See https://confluence.adeo.no/pages/viewpage.action?pageId=210440645 and its child pages.

Pro-tip: use [nais-cli](https://github.com/nais/naisd). 

### Docker

Utvikler-image (Windows) no-go due to disabled virtualization flags. Need access to Linux image.
Repo available at repo.adeo.no:5443.
Deployment on local machine is possible. Alternatively, provision a Linux server (or VDI) for 
building the Docker images.

* Build a JAR and output it in ```target``` subdirectory.
* Build Docker image using Dockerfile.
* Push to docker.adeo.no

```
docker build -f Dockerfile -t altinnkanal .
```

Run
```
docker run --rm -p 8080:8080 -t altinnkanal
```

If "port already allocated" errors, find and stop existing containers:
```
docker ps
```

and

```
docker stop <CONTAINER_NAMES>
```

### Testing against Kafka test-rig
IPs and hostnames should be available on the #kafka Slack channel. Still WIP so they'll probably change.
