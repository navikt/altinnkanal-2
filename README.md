# Altinnkanal 2

Repository for Altinnkanal 2. Application written in Kotlin to handle routing of messages from Altinn through a webservice
 endpoint to internal systems using Kafka. 

## Technologies & Tools

* Kotlin
* Kafka
* CXF
* Jetty
* Gradle
* Spek

## Getting started

### Build and run tests
`./gradlew clean build`

### Compile and build JARs + startup scripts:

`./gradlew clean installDist`

The files needed for distribution should then be available under 

`./altinnkanal/build/install/`

with startup scripts under:

`./altinnkanal/build/install/altinnkanal/bin/`


### Running locally

The application assumes you've set certain environment variables:

* The following are used for WS-Security validation. The webservice will validate
  UsernameTokens in the SOAP headers against LDAP. 
    * `LDAP_URL`
    * `LDAP_USERNAME` - username for initial lookup (should not be the same as the one in the SOAP request).
    * `LDAP_PASSWORD`
    * `LDAP_SERVICEUSER_BASEDN` - the base distinguished name for lookup.
    * `LDAP_AD_GROUP` - the group that user should be a member of.

* `SRVALTINNKANAL_USERNAME` and `SRVALTINNKANAL_PASSWORD` - required for the JAAS
configuration used when connecting to Kafka.

A Webservice (as defined by the WSDL in `src/main/resources/OnlineBatchReceiver.wsdl`) should be available 
at: http://localhost:8080/webservices/OnlineBatchReceiverSoap

### Contact us
#### Code/project related questions can be sent to 
* Kevin Sillerud, `kevin.sillerud@nav.no`
* Trong Huu Nguyen, `trong.huu.nguyen@nav.no`

#### For NAV employees
We are also available on the slack channel #integrasjon for internal communication.

##### Testing against Kafka
IPs and hostnames for testing should be available in the #kafka Slack channel. Still WIP so they'll probably change. 
You might want to consider using a Docker image or running the 
[Confluent Open Source distribution](https://www.confluent.io/product/confluent-open-source/) locally.

