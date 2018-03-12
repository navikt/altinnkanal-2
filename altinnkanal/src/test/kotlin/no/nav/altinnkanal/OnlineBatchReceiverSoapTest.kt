package no.nav.altinnkanal

import java.util.concurrent.Future
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class OnlineBatchReceiverSoapTest {
    @Mock
    private lateinit var topicService: TopicService
    @Mock
    private lateinit var kafkaProducer: Producer<String, ExternalAttachment>
    private lateinit var soapService: OnlineBatchReceiverSoap
    private lateinit var simpleBatch: String
    @Mock
    private lateinit var metadataFuture: Future<RecordMetadata>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        soapService = OnlineBatchReceiverSoapImpl(topicService, kafkaProducer)
        simpleBatch = Utils.readToString("/data/basic_data_batch.xml")
    }

    @Test
    @Throws(Exception::class)
    fun testFailedMissingTopic() {
        `when`<String>(topicService.getTopic(anyString(), anyString())).thenReturn(null)
        assertEquals("FAILED_DO_NOT_RETRY", soapService
                .receiveOnlineBatchExternalAttachment("username", "password",
                        "123uhjoas", 0, simpleBatch, ByteArray(0)))
    }

    @Test
    @Throws(Exception::class)
    fun testValidCall() {
        `when`<String>(topicService.getTopic(anyString(), anyString())).thenReturn("test")
        `when`(metadataFuture.get()).thenReturn(mock(RecordMetadata::class.java))
        `when`(kafkaProducer.send(any())).thenReturn(metadataFuture)
        assertEquals("OK", soapService
                .receiveOnlineBatchExternalAttachment("username", "password",
                        "123uhjoas", 0, simpleBatch, ByteArray(0)))
    }
}
