package no.nav.altinnkanal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Future
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.avro.ReceivedMessage
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.altinnkanal.soap.Status
import org.amshove.kluent.shouldEqual
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RecordMetadata
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object OnlineBatchReceiverSoapSpec : Spek({
    val topicService = mock<TopicService>()
    val kafkaProducer = mock<Producer<String, ExternalAttachment>>()
    val kafkaProducer2 = mock<Producer<String, ReceivedMessage>>()
    val metadataFuture = mock<Future<RecordMetadata>>()
    val soapService = OnlineBatchReceiverSoapImpl(topicService, kafkaProducer, kafkaProducer2)
    val simpleBatch = "/data/basic_data_batch.xml".getResource()

    whenever(metadataFuture.get()).thenReturn(mock())
    whenever(kafkaProducer.send(any())).thenReturn(metadataFuture)

    describe("receiveOnlineBatchExternalAttachment") {
        listOf(
            Triple("valid topic routing", listOf("test"), Status.OK.name),
            Triple("missing topic routing", null, Status.FAILED_DO_NOT_RETRY.name)
        ).forEach { (description, validTopics, expected) ->
            context(description) {
                it("should return $expected for batch") {
                    whenever(topicService.getTopics(any(), any())).thenReturn(validTopics)
                    val result = soapService.receiveOnlineBatchExternalAttachment(
                        username = null,
                        passwd = null,
                        receiversReference = null,
                        sequenceNumber = 0,
                        dataBatch = simpleBatch,
                        attachments = ByteArray(0)
                    ).getResultCode()
                    result shouldEqual expected
                }
            }
        }
    }
})
