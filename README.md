# Altinnkanal - Spring

## Docker notes


Utvikler-image (Windows) no-go due to disabled virtualization flags. Need access to Linux image.
Repo available at docker.adeo.no:5000, browsable at https://registry-browser.adeo.no/home.
Deployment on local machine is possible.

### Build
Checklist:
* Build a JAR with all dependencies, e.g. using maven-shade-plugin or maven-assembly-plugin.
* Make sure the JAR is output in the ```target``` subdirectory.
* Make sure the JAR has a name matching the one defined in the Dockerfile.

```
docker build -f Dockerfile -t altinnkanal .
```

### Run
```
docker run --rm -p 8080:8080 -t altinnkanal
```

### If "port already allocated" errors, find and stop existing containers:
```
docker ps
```

and

```
docker stop <CONTAINER_NAMES>
```
