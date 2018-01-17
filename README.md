# Altinnkanal 2

This is the repository for the new Altinnkanal using Spring Boot and Kafka (Confluent Platform), aiming for deployment on NAIS.

## Prerequisites

* Java 1.8
* Maven 3.3+

For deployment:
* Docker (tested on 17.03.1-ce)

## Notes on local development

### Build, test, run and deploy
Generally, we recommend just checking in code to the repository for packaging and deployment as our Jenkins CI server automatically handles this.

Nonetheless, notes on the process are available in the following sections.

#### Environment variables
Certain environment variables must be set before running:

```SPRING_PROFILES_ACTIVE=local```

And,

```LDAP_URL, LDAP_USER_BASEDN, LDAP_USERNAME and LDAP_PASSWORD```

must also be set. 

Reference Fasit for the values associated to the environment you're running in. 

While developing locally, we've usually used *.test.local (i.e. values for the T-environments).

#### Build & run

Make sure to run to generate required code:

```mvn clean generate-sources```

Then, compile and run as you please.

#### Test

Run unit and integration tests:

```mvn clean verify```

#### Package

Create a runnable JAR:

```mvn clean package -DskipTests```

Create a Docker image using the included Dockerfile (see the notes below on Docker if you're on Windows):

```
docker build -f path\to\Dockerfile -t integrasjon/altinnkanal-2:<version>

docker tag integrasjon/altinnkanal-2:<version> docker.adeo.no:5000/integrasjon/altinnkanal-2:<version>

docker push docker.adeo.no:5000/integrasjon/altinnkanal-2:<version>
```

#### Deploy

Deployment to NAIS. See https://confluence.adeo.no/pages/viewpage.action?pageId=210440645 and its child pages.

Pro-tip: use [nais-cli](https://github.com/nais/naisd). 

### Prometheus
Add

```
- job_name: 'altinnkanal'
  
    static_configs:
      - targets: ['localhost:8080']
 
    metrics_path: /prometheus
```
to ```prometheus.yml``` under ```scrape_configs```.

### Docker

Utvikler-image (Windows) no-go due to disabled virtualization flags. Need access to Linux image.
Repo available at docker.adeo.no:5000, browsable at https://registry-browser.adeo.no/home.
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

#### SSL

Broker connection requires SSL (and probably some form of auth in the future).
Connection to schema-registry also requires SSL.

This will cause missing certs exception in JVM.

To fix: Set up your own truststore (using the Java `keytool` utility) with the NAV root certificates - 
available [here](https://confluence.adeo.no/display/ITOSS/Root-sertifikater).

Add these options to the VM:

```
-Djavax.net.ssl.trustStore=path\to\<truststore>.jks
-Djavax.net.ssl.trustStorePassword=<truststore password>
```
