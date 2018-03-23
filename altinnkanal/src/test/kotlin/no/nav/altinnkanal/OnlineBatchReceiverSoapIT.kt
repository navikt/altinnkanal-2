package no.nav.altinnkanal

import java.io.IOException
import java.net.ServerSocket
import java.util.HashMap
import java.util.Properties
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.UnsupportedCallbackException
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.config.SoapProperties
import no.nav.altinnkanal.config.*
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.services.TopicServiceTest
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.common.KafkaEnvironment
import no.nav.common.embeddedutils.ServerBase
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.eclipse.jetty.server.Server
import org.junit.Assert.assertEquals
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class OnlineBatchReceiverSoapIT {

    private lateinit var simpleBatch: String
    private lateinit var simpleBatchMissingSec: String

    private fun createPayload(serviceCode: String, serviceEditionCode: String): String {
        return simpleBatch
                .replace("\\{\\{serviceCode}}".toRegex(), serviceCode)
                .replace("\\{\\{serviceEditionCode}}".toRegex(), serviceEditionCode)
    }

    class ClientPasswordCallback : CallbackHandler {
        @Throws(IOException::class, UnsupportedCallbackException::class)
        override fun handle(callbacks: Array<Callback>) {
            val pc = callbacks[0] as WSPasswordCallback
            pc.password = soapProperties.password
        }
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        simpleBatch = Utils.readToString("/data/basic_data_batch.xml")
        simpleBatchMissingSec = Utils.readToString("/data/basic_data_batch_missing_sec.xml")

        soapEndpoint = JaxWsProxyFactoryBean().apply {
            serviceClass = OnlineBatchReceiverSoap::class.java
            address = "http://localhost:$localServerPort/webservices/OnlineBatchReceiverSoap"
            create()
        } as OnlineBatchReceiverSoap

        val outProps = HashMap<String, Any>().apply {
            put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN)
            put(WSHandlerConstants.USER, soapProperties.username)
            put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT)
            put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordCallback::class.java.name)
        }
        ClientProxy.getClient(soapEndpoint).run {
            outInterceptors.add(WSS4JOutInterceptor(outProps))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testValidScSec() {
        val payload = createPayload("2896", "87")
        val result = soapEndpoint.receiveOnlineBatchExternalAttachment(
                null, null, null, 0, payload, ByteArray(0))
        assertEquals("OK", result)
    }

    @Test
    @Throws(Exception::class)
    fun testNonRoutedScSec() {
        val serviceCode = "1233"
        val serviceEditionCode = "1"
        val payload = createPayload(serviceCode, serviceEditionCode)
        val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null, null,
                0, payload, ByteArray(0))
        assertEquals("FAILED_DO_NOT_RETRY", result)
    }

    @Test
    @Throws(Exception::class)
    fun testMissingSecInPayload() {
        val result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null, null,
                0, simpleBatchMissingSec, ByteArray(0))
        assertEquals("FAILED", result)
    }

    @Test
    @Throws(Exception::class)
    fun testKafkaBrokerTemporarilyUnavailable() {
        val serviceCode = "2896"
        val serviceEditionCode = "87"
        val payload = createPayload(serviceCode, serviceEditionCode)

        // shutdown embedded Kafka
        kafkaEnvironment.serverPark.brokers.forEach(ServerBase::stop)
        var result: String = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null, null,
                0, payload, ByteArray(0))
        assertEquals("FAILED", result)

        println("NEXT TEST")
        // restart embedded Kafka
        kafkaEnvironment.serverPark.brokers.forEach(ServerBase::start)
        result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null, null,
                0, payload, ByteArray(0))
        assertEquals("OK", result)
    }

    companion object {
        private val soapProperties = SoapProperties("test", "test")
        private var localServerPort: Int = 0
        private lateinit var soapEndpoint: OnlineBatchReceiverSoap
        private lateinit var kafkaEnvironment: KafkaEnvironment

        val randomOpenPort: Int
            @Throws(IOException::class)
            get() = ServerSocket(-1).use { socket -> return socket.localPort }

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun setupClass() {
            kafkaEnvironment = KafkaEnvironment(1, listOf("aapen-altinn-bankkontonummerv87Mottatt-v1-preprod"),
                    true, false, false)
                    .apply {
                        start()
                    }
            val producer = KafkaProducer<String, ExternalAttachment>(kafkaProperties())
            val topicService = TopicService(topicRouting())
            val batchReceiver = OnlineBatchReceiverSoapImpl(topicService, producer)
            //localServerPort = getRandomOpenPort();
            localServerPort = 8123
            val server = Server(localServerPort)
            bootstrap(server, soapProperties, batchReceiver)
        }

        @AfterClass
        @JvmStatic
        @Throws(Exception::class)
        fun tearDownClass() {
            kafkaEnvironment.tearDown()
        }

        @JvmStatic
        @Throws(Exception::class)
        private fun kafkaProperties(): Properties {
            return Properties().apply {
                load(TopicServiceTest::class.java.getResourceAsStream("/kafka.properties"))
                setProperty("bootstrap.servers", kafkaEnvironment.brokersURL)
                setProperty("schema.registry.url", kafkaEnvironment.serverPark.schemaregistry.url)
                setProperty("request.timeout.ms", "1000")
                remove("security.protocol")
            }
        }
    }
}
