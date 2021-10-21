package no.nav.altinnkanal.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Future
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.avro.ReceivedMessage
import no.nav.altinnkanal.getResource
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeIn
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object KafkaServiceSpec : Spek({
    val topicRepository = mock<TopicService>()
    val kafkaProducer = mock<Producer<String, ExternalAttachment>>()
    val kafkaProducer2 = mock<Producer<String, ReceivedMessage>>()
    val future = mock<Future<RecordMetadata>>()
    val recordMetadata = mock<RecordMetadata>()
    val captor = argumentCaptor<ProducerRecord<String, ExternalAttachment>>()

    val onlineBatchReceiver = OnlineBatchReceiverSoapImpl(topicRepository, kafkaProducer, kafkaProducer2)
    val expectedTopics = listOf("topic1", "topic2")

    whenever(recordMetadata.serializedValueSize()).thenReturn(0)
    whenever(future.get()).thenReturn(recordMetadata)
    whenever(kafkaProducer.send(captor.capture())).thenReturn(future)
    whenever(topicRepository.getTopics(any(), any())).thenReturn(expectedTopics)

    describe("a valid data batch") {
        val simpleBatch = "/data/basic_data_batch.xml".getResource()
        context("receiveOnlineBatchExternalAttachment") {
            onlineBatchReceiver.receiveOnlineBatchExternalAttachment(
                username = null,
                passwd = null,
                receiversReference = null,
                sequenceNumber = 0,
                dataBatch = simpleBatch,
                attachments = ByteArray(0)
            )
            val record = captor.firstValue
            val record2 = captor.secondValue
            it("should invoke the Kafka Producer twice") {
                verify(kafkaProducer, times(2)).send(any())
            }
            it("should send the message to the correct topics") {
                record.topic() shouldBeIn expectedTopics
                record2.topic() shouldBeIn expectedTopics
                record.topic() shouldBe expectedTopics.first()
                record2.topic() shouldBe expectedTopics[1]
            }
        }
    }
})
