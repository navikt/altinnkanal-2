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

### Running locally with Docker

Utvikler-image (Windows) no-go due to disabled virtualization flags. Need access to Linux image.
Deployment on local machine is possible. Alternatively, provision a Linux server (or VDI) for 
building the Docker images.

* Build a JAR and output it in `target` subdirectory.
* Build Docker image using Dockerfile.
* Run the container and delete on exit.

#### Compile and build JARs + startup scripts:

`./gradlew clean installDist`

#### Build docker container
`docker build -f Dockerfile -t altinnkanal-2 .`

#### Run
`docker run --rm -p 8080:8080 -it altinnkanal-2`

#### If "port already allocated" errors, find and stop existing containers:
`docker ps` then `docker stop <CONTAINER_NAMES>`

### Testing against Kafka test-rig
IPs and hostnames should be available on the #kafka Slack channel. Still WIP so they'll probably change. 
You might want to consider using a docker image or running the open source confluent suite locally.

### Contact us
#### Code/project related questions can be sent to 
* Kevin Sillerud, `kevin.sillerud@nav.no`
* Trong Huu Nguyen, `trong.huu.nguyen@nav.no`

#### For NAV employees
We are also available on the slack channel #integrasjon for internal communication.
