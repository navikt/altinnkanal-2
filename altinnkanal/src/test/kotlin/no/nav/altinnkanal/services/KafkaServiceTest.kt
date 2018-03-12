package no.nav.altinnkanal.services

import java.util.concurrent.Future
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.Utils
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class KafkaServiceTest {
    @Mock
    private lateinit var topicRepository: TopicService
    private lateinit var onlineBatchReceiver: OnlineBatchReceiverSoap
    @Mock
    private lateinit var kafkaProducer: Producer<String, ExternalAttachment>
    @Mock
    private lateinit var future: Future<RecordMetadata>
    @Captor
    private lateinit var argumentCaptor: ArgumentCaptor<ProducerRecord<String, ExternalAttachment>>
    private lateinit var simpleBatch: String

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        simpleBatch = Utils.readToString("/data/basic_data_batch.xml")
        onlineBatchReceiver = OnlineBatchReceiverSoapImpl(topicRepository, kafkaProducer)
    }

    @Test
    @Throws(Exception::class)
    fun testPublishedToKafka() {
        println(kafkaProducer)
        val expectedTopic = "test.test"

        `when`<String>(topicRepository.getTopic(anyString(), anyString())).thenReturn(expectedTopic)

        val recordMetadata = mock(RecordMetadata::class.java)
        `when`(recordMetadata.serializedValueSize()).thenReturn(0)

        `when`(future.get()).thenReturn(recordMetadata)

        `when`(kafkaProducer.send(argumentCaptor.capture())).thenReturn(future)

        onlineBatchReceiver.receiveOnlineBatchExternalAttachment("", "", "",
                0, simpleBatch, ByteArray(0))

        val record = argumentCaptor.value

        verify(kafkaProducer, times(1)).send(any())
        assertEquals(expectedTopic, record.topic())
    }
}
