package no.nav.altinnkanal.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Future
import no.nav.altinnkanal.Utils
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.amshove.kluent.shouldBe
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object KafkaServiceSpec : Spek({
    val topicRepository = mock<TopicService>()
    val kafkaProducer = mock<Producer<String, ExternalAttachment>>()
    val future = mock<Future<RecordMetadata>>()
    val recordMetadata = mock<RecordMetadata>()
    val captor = argumentCaptor<ProducerRecord<String, ExternalAttachment>>()

    val onlineBatchReceiver = OnlineBatchReceiverSoapImpl(topicRepository, kafkaProducer)
    val expectedTopic = "test.test"

    whenever(recordMetadata.serializedValueSize()).thenReturn(0)
    whenever(future.get()).thenReturn(recordMetadata)
    whenever(kafkaProducer.send(captor.capture())).thenReturn(future)
    whenever(topicRepository.getTopic(any(), any())).thenReturn(expectedTopic)

    given("a valid data batch") {
        val simpleBatch = Utils.readToString("/data/basic_data_batch.xml")
        on("receiveOnlineBatchExternalAttachment") {
            onlineBatchReceiver.receiveOnlineBatchExternalAttachment("", "", "", "",
                    "", 0, simpleBatch, null, ByteArray(0))
            val record = captor.firstValue
            it("it should invoke the Kafka Producer once") {
                verify(kafkaProducer, times(1)).send(any())
            }
            it("it should send the message to the correct topic") {
                record.topic() shouldBe expectedTopic
            }
        }
    }
})
