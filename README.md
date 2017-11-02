# Altinnkanal 2

This is the repository for the new Altinnkanal using Spring Boot and Kafka (Confluent Platform), aiming for deployment on NAIS.

## Notes on local development

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
Deployment on local machine is possible.

* Build a JAR and output it in ```target``` subdirectory.
* Build Docker image using Dockerfile.
* Push to docker.adeo.no (?)

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

#### SSL/auth for broker connection 
Will likely be added in the future. Should reference https://docs.confluent.io/current/kafka/ssl.html for proper client configuration.

#### Connection to schema-registry might require SSL
Causes missing certs exception in JVM.

Fix: https://stackoverflow.com/questions/21076179/pkix-path-building-failed-and-unable-to-find-valid-certification-path-to-requ

Basically:
* Find the ```cacerts``` file for your JRE (e.g. ```C:\java\jdk8\jre\lib\security```)
* Export the certificate for the host of the schema-registry (using e.g. Firefox or Chrome)
* Import the certificate into the cacerts using keytool (default password is ```changeit```:
```
keytool -import -alias <example> -keystore  <path_to_cacerts> -file <path_to_exported_cert>.cer
```
* Restart JVM/PC
