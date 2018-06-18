package no.nav.altinnkanal

import no.altinn.webservices.ReceiveOnlineBatchExternalAttachment as ROBEA
import java.util.Properties
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.Status
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

    val kafkaEnvironment = KafkaEnvironment(
        noOfBrokers = 1,
        topics = listOf("aapen-altinn-bankkontonummer-Mottatt"),
        withSchemaRegistry = true,
        withRest = false,
        autoStart = true
    )
    val kafkaProperties = Properties().apply {
        load("/kafka.properties".getResourceStream())
        setProperty("bootstrap.servers", kafkaEnvironment.brokersURL)
        setProperty("schema.registry.url", kafkaEnvironment.serverPark.schemaregistry.url)
        setProperty("request.timeout.ms", "1000")
        remove("security.protocol")
    }
    val producer = KafkaProducer<String, ExternalAttachment>(kafkaProperties)

    val ldapServer = createLdapServer()

    val localServerPort = 47859
    val server = Server(localServerPort).apply {
        bootstrap(this, OnlineBatchReceiverSoapImpl(TopicService(), producer))
    }
    context("SOAP requests") {
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
                    val client = createSoapClient(localServerPort, username, password)
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
            val soapClient = createSoapClient(localServerPort, "srvaltinnkanal",
                "supersecurepassword")
            on("a payload with %s",
                data("valid combination of SC and SEC",
                    createPayload(simpleBatch, "2896", "87"),
                    expected = Status.OK.name),
                data("non-routed combination of SC and SEC",
                    createPayload(simpleBatch, "1233", "1"),
                    expected = Status.FAILED_DO_NOT_RETRY.name),
                data("missing SC and/or SEC",
                    "/data/basic_data_batch_missing_sec.xml".getResource(),
                    expected = Status.FAILED.name)
            ) { _, payload, expected ->

                it("should return a result equal to $expected for batch") {
                    val result = soapClient.receiveOnlineBatchExternalAttachment(
                        ROBEA().apply {
                            sequenceNumber = 0
                            batch = payload
                        }).receiveOnlineBatchExternalAttachmentResult.getResultCode()

                    result shouldEqual expected
                }

                it("should return a result equal to $expected for Batch") {
                    val result = soapClient.receiveOnlineBatchExternalAttachment(
                        ROBEA().apply {
                            sequenceNumber = 0
                            batch1 = payload
                        }).receiveOnlineBatchExternalAttachmentResult.getResultCode()

                    result shouldEqual expected
                }
            }

            on("a payload with valid combination of SC and SEC") {
                val payload = createPayload(simpleBatch, "2896", "87")

                on("%s",
                    data("Kafka temporarily down", ServerBase::stop, expected = Status.FAILED),
                    data("Kafka back up again", ServerBase::start, expected = Status.OK)
                ) { _, operation, expected ->
                    kafkaEnvironment.serverPark.brokers.forEach(operation)

                    it("should return a result equal to $expected for batch") {
                        val result = soapClient.receiveOnlineBatchExternalAttachment(
                            ROBEA().apply {
                                sequenceNumber = 0
                                batch = payload
                            }
                        ).receiveOnlineBatchExternalAttachmentResult.getResultCode()

                        result shouldEqual expected
                    }

                    it("should return a result equal to $expected for Batch") {
                        val result = soapClient.receiveOnlineBatchExternalAttachment(
                            ROBEA().apply {
                                sequenceNumber = 0
                                batch1 = payload
                            }
                        ).receiveOnlineBatchExternalAttachmentResult.getResultCode()

                        result shouldEqual expected
                    }
                }
            }
        }
    }

    context("self tests") {
        on("%s",
            data("is_alive", expected = APPLICATION_ALIVE),
            data("is_ready", expected = APPLICATION_READY)
        ) { path, expected ->
            val conn = URL("http://localhost:$localServerPort/internal/$path").let {
                it.openConnection() as HttpURLConnection
            }

            it("should return HTTP 200 OK") {
                conn.responseCode shouldEqual HttpURLConnection.HTTP_OK
            }

            it("should return $expected") {
                val response = Scanner(
                    URL("http://localhost:$localServerPort/internal/$path").openStream(), "UTF-8")
                    .useDelimiter("\\n").next()

                response shouldEqual expected
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
