package no.nav.altinnkanal

import no.altinn.webservices.ReceiveOnlineBatchExternalAttachment as ROBEA
import java.util.Properties
import no.nav.altinnkanal.Utils.createPayload
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.rest.APPLICATION_ALIVE
import no.nav.altinnkanal.rest.APPLICATION_READY
import no.nav.altinnkanal.rest.SelfTest
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.FAILED
import no.nav.altinnkanal.soap.FAILED_DO_NOT_RETRY
import no.nav.altinnkanal.soap.OK
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.common.KafkaEnvironment
import no.nav.common.embeddedutils.ServerBase
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withCause
import org.apache.cxf.binding.soap.SoapFault
import org.apache.kafka.clients.producer.KafkaProducer
import org.eclipse.jetty.server.Server
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.data_driven.data
import org.jetbrains.spek.data_driven.on
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

object OnlineBatchReceiverSoapITSpec : Spek({
    val simpleBatch = "/data/basic_data_batch.xml".getResource()
    val kafkaEnvironment = KafkaEnvironment(1, listOf("aapen-altinn-bankkontonummerv87Mottatt-v1-preprod"),
        true, false, false)
        .apply {
            start()
        }
    val kafkaProperties = Properties().apply {
        load(OnlineBatchReceiverSoapITSpec::class.java.getResourceAsStream("/kafka.properties"))
        setProperty("bootstrap.servers", kafkaEnvironment.brokersURL)
        setProperty("schema.registry.url", kafkaEnvironment.serverPark.schemaregistry.url)
        setProperty("request.timeout.ms", "1000")
        remove("security.protocol")
    }
    val producer = KafkaProducer<String, ExternalAttachment>(kafkaProperties)
    val localServerPort = 8080
    // val localServerPort = ServerSocket(-1).run { this.localPort }
    val server = Server(localServerPort).apply {
        bootstrap(this, OnlineBatchReceiverSoapImpl(
            TopicService(), producer)
        )
    }
    val ldapServer = Utils.createLdapServer()

    given("invalid username tokens") {
        val payload = createPayload(simpleBatch, "2896", "87")
        on("usernametoken with %s",
            data("invalid password", "srvaltinnkanal", "wrongpassword", expected = SoapFault::class),
            data("valid credentials but missing AD-group membership", "srvnotaltinnkanal", "notpassword",
                expected = SoapFault::class),
            data("non-existent username", "altinnkanal", "password", expected = SoapFault::class),
            data("invalid username / ldap query injection", ") (&(cn=srvnotaltinnkanal)", "password",
                expected = SoapFault::class)
        ) { _, username, password, expected ->
            it("should throw a SOAPFaultException") {
                val client = Utils.createSoapClient(localServerPort, username, password)
                val result =
                    { client.receiveOnlineBatchExternalAttachment(
                        ROBEA().apply {
                            sequenceNumber = 0
                            batch = payload
                        })
                    }
                result shouldThrow Exception::class withCause expected
            }
        }
    }
    given("valid usernametoken") {
        val soapClient = Utils.createSoapClient(localServerPort, "srvaltinnkanal",
            "supersecurepassword")
        on("a payload with %s",
            data("valid combination of SC and SEC",
                createPayload(simpleBatch, "2896", "87"),
                expected = OK),
            data("non-routed combination of SC and SEC",
                createPayload(simpleBatch, "1233", "1"),
                expected = FAILED_DO_NOT_RETRY),
            data("missing SC and/or SEC",
                "/data/basic_data_batch_missing_sec.xml".getResource(),
                expected = FAILED)
        ) { _, payload, expected ->
            it("should return a result equal to $expected for batch") {
                val result = soapClient.receiveOnlineBatchExternalAttachment(
                    ROBEA().apply {
                        sequenceNumber = 0
                        batch = payload
                }).receiveOnlineBatchExternalAttachmentResult
                result shouldEqual expected
            }
            it("should return a result equal to $expected for Batch") {
                val result = soapClient.receiveOnlineBatchExternalAttachment(
                    ROBEA().apply {
                        sequenceNumber = 0
                        batch1 = payload
                }).receiveOnlineBatchExternalAttachmentResult
                result shouldEqual expected
            }
        }
        on("a payload with valid combination of SC and SEC") {
            val payload = createPayload(simpleBatch, "2896", "87")
            on("%s",
                data("Kafka temporarily down", ServerBase::stop, expected = FAILED),
                data("Kafka back up again", ServerBase::start, expected = OK)
            ) { _, operation, expected ->
                kafkaEnvironment.serverPark.brokers.forEach(operation)
                it("should return a result equal to $expected for batch") {
                    val result = soapClient.receiveOnlineBatchExternalAttachment(
                        ROBEA().apply {
                            sequenceNumber = 0
                            batch = payload
                        }
                    ).receiveOnlineBatchExternalAttachmentResult
                    result shouldEqual expected
                }
                it("should return a result equal to $expected for Batch") {
                    val result = soapClient.receiveOnlineBatchExternalAttachment(
                        ROBEA().apply {
                            sequenceNumber = 0
                            batch1 = simpleBatch
                        }
                    ).receiveOnlineBatchExternalAttachmentResult
                    result shouldEqual expected
                }
            }
        }
    }
    context("SelfTest Connection") {
        on("%s",
            data("isAlive", expected = APPLICATION_ALIVE),
            data("isReady", expected = APPLICATION_READY)
        ) { path, expected ->
            val conn = URL("http://localhost:$localServerPort/$path").let {
                it.openConnection() as HttpURLConnection
            }
            it("should return HTTP 200 OK") {
                conn.responseCode shouldEqual HttpURLConnection.HTTP_OK
            }
            it("should return $expected") {
                val response = Scanner(
                    URL("http://localhost:$localServerPort/$path").openStream(), "UTF-8")
                    .useDelimiter("\\A").next()
                response shouldEqual expected
            }
        }
    }
    context("SelfTest Classes") {
        on("getIsAlive") {
            val expected = APPLICATION_ALIVE
            it("should return $expected") {
                SelfTest().getIsAlive() shouldEqual expected
            }
        }
        on("getIsReady") {
            val expected = APPLICATION_READY
            it("should return $expected") {
                SelfTest().getIsReady().readEntity(String::class.java) shouldEqual expected
            }
        }
    }

    afterGroup {
        kafkaEnvironment.tearDown()
        producer.run {
            flush()
            close()
        }
        ldapServer.shutDown(true)
        server.stop()
    }
})
