package no.nav.altinnkanal

import java.util.HashMap
import java.util.Properties
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.Utils.createPayload
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.config.topicRouting
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.services.TopicServiceSpec
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.common.KafkaEnvironment
import no.nav.common.embeddedutils.ServerBase
import org.amshove.kluent.`should equal`
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.eclipse.jetty.server.Server
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

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

    val soapEndpoint = JaxWsProxyFactoryBean().run {
        serviceClass = OnlineBatchReceiverSoap::class.java
        address = "http://localhost:$localServerPort/webservices/OnlineBatchReceiverSoap"
        create() as OnlineBatchReceiverSoap
    }
    bootstrap(server, batchReceiver)

    given("a payload with valid combination of SC and SEC") {
        val payload = createPayload(simpleBatch, "2896", "87")
        on("receiveOnlineBatchExternalAttachment") {
            val result = soapEndpoint.receiveOnlineBatchExternalAttachment(
                    null, null, null, 0, payload, ByteArray(0))
            it("should return a result equal to OK") {
                result `should equal` "OK"
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
                result `should equal` "FAILED_DO_NOT_RETRY"
            }
        }
    }

    given("a payload with missing SC and/or SEC") {
        val simpleBatchMissingSec = Utils.readToString("/data/basic_data_batch_missing_sec.xml")
        on("receiveOnlineBatchExternalAttachment") {
            val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                    null, 0, simpleBatchMissingSec, ByteArray(0))
            it("should return a result equal to FAILED") {
                result `should equal` "FAILED"
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
                result `should equal` "FAILED"
            }
        }
        on("receiveOnlineBatchExternalAttachment and Kafka back up again") {
            kafkaEnvironment.serverPark.brokers.forEach(ServerBase::start)
            val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                    null, 0, payload, ByteArray(0))
            it("return a result equal to OK") {
                result `should equal` "OK"
            }
        }
    }

    afterGroup {
        kafkaEnvironment.tearDown()
        producer.apply {
            flush()
            close()
        }
        server.stop()
    }
})
