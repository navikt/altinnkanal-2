package no.nav.altinnkanal

import java.util.Properties
import no.nav.altinnkanal.Utils.createPayload
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.config.topicRouting
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.services.TopicServiceSpec
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.common.KafkaEnvironment
import no.nav.common.embeddedutils.ServerBase
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.apache.kafka.clients.producer.KafkaProducer
import org.eclipse.jetty.server.Server
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import javax.xml.ws.soap.SOAPFaultException

class OnlineBatchReceiverSoapITSpec: Spek({
    val simpleBatch = Utils.readToString("/data/basic_data_batch.xml")
    val kafkaEnvironment = KafkaEnvironment(1, listOf("aapen-altinn-bankkontonummerv87Mottatt-v1-preprod"),
        true, false, false)
        .apply {
            start()
        }
    val kafkaProperties = Properties().apply {
        load(TopicServiceSpec::class.java.getResourceAsStream("/kafka.properties"))
        setProperty("bootstrap.servers", kafkaEnvironment.brokersURL)
        setProperty("schema.registry.url", kafkaEnvironment.serverPark.schemaregistry.url)
        setProperty("request.timeout.ms", "1000")
        remove("security.protocol")
    }

    val topicService = TopicService(topicRouting())
    val producer = KafkaProducer<String, ExternalAttachment>(kafkaProperties)
    val batchReceiver = OnlineBatchReceiverSoapImpl(topicService, producer)

    val localServerPort = 8123
    //val localServerPort = ServerSocket(-1).run { this.localPort }
    val server = Server(localServerPort)

    Utils.createLdapServer()

    bootstrap(server, batchReceiver)

    given("invalid usernametokens") {
        val payload = createPayload(simpleBatch, "2896", "87")
        given("usernametoken with invalid password") {
            val soapEndpoint = Utils.createSoapEndpoint(localServerPort, "srvaltinnkanal", "wrongpassword")
            it("should throw a SOAPFaultException") {
                val result = { soapEndpoint.receiveOnlineBatchExternalAttachment(
                        null, null, null, 0, payload, ByteArray(0)) }
                result shouldThrow SOAPFaultException::class
            }
        }
        given("usernametoken with valid credentials but missing AD-group membership") {
            val soapEndpoint = Utils.createSoapEndpoint(localServerPort, "srvnotalinnkanal", "notpassword")
            it("should throw a SOAPFaultException") {
                val result = { soapEndpoint.receiveOnlineBatchExternalAttachment(
                        null, null, null, 0, payload, ByteArray(0)) }
                result shouldThrow SOAPFaultException::class
            }
        }
        given("usernametoken with non-existent username") {
            val soapEndpoint = Utils.createSoapEndpoint(localServerPort, "altinnkanal", "password")
            it("should throw a SOAPFaultException") {
                val result = { soapEndpoint.receiveOnlineBatchExternalAttachment(
                        null, null, null, 0, payload, ByteArray(0)) }
                result shouldThrow SOAPFaultException::class
            }
        }
    }

    given("valid usernametokens") {
        val soapEndpoint = Utils.createSoapEndpoint(localServerPort,
                "srvaltinnkanal", "supersecurepassword")
        given("a payload with valid combination of SC and SEC") {
            val payload = createPayload(simpleBatch, "2896", "87")
            on("receiveOnlineBatchExternalAttachment") {
                val result = soapEndpoint.receiveOnlineBatchExternalAttachment(
                        null, null, null, 0, payload, ByteArray(0))
                it("should return a result equal to OK") {
                    result shouldEqual "OK"
                }
            }
        }
        given("a payload with non-routed combination of SC and SEC") {
            val serviceCode = "1233"
            val serviceEditionCode = "1"
            val payload = createPayload(simpleBatch, serviceCode, serviceEditionCode)
            on("receiveOnlineBatchExternalAttachment") {
                val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                        null, 0, payload, ByteArray(0))
                it("should return a result equal to FAILED_DO_NOT_RETRY") {
                    result shouldEqual "FAILED_DO_NOT_RETRY"
                }
            }
        }
        given("a payload with missing SC and/or SEC") {
            val simpleBatchMissingSec = Utils.readToString("/data/basic_data_batch_missing_sec.xml")
            on("receiveOnlineBatchExternalAttachment") {
                val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                        null, 0, simpleBatchMissingSec, ByteArray(0))
                it("should return a result equal to FAILED") {
                    result shouldEqual "FAILED"
                }
            }
        }
        given("a valid payload") {
            val serviceCode = "2896"
            val serviceEditionCode = "87"
            val payload = createPayload(simpleBatch, serviceCode, serviceEditionCode)
            on("receiveOnlineBatchExternalAttachment and Kafka temporarily down") {
                kafkaEnvironment.serverPark.brokers.forEach(ServerBase::stop)
                val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                        null, 0, payload, ByteArray(0))
                it("should return a result equal to FAILED") {
                    result shouldEqual "FAILED"
                }
            }
            on("receiveOnlineBatchExternalAttachment and Kafka back up again") {
                kafkaEnvironment.serverPark.brokers.forEach(ServerBase::start)
                val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                        null, 0, payload, ByteArray(0))
                it("return a result equal to OK") {
                    result shouldEqual "OK"
                }
            }
        }
    }

    afterGroup {
        kafkaEnvironment.tearDown()
        producer.run {
            flush()
            close()
        }
        Utils.shutdownLdapServer()
        server.stop()
    }
})
