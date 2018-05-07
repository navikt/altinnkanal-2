package no.nav.altinnkanal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Future
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.amshove.kluent.shouldBe
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RecordMetadata
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class OnlineBatchReceiverSoapSpec: Spek({
    val topicService = mock<TopicService>()
    val kafkaProducer = mock<Producer<String, ExternalAttachment>>()
    val metadataFuture = mock<Future<RecordMetadata>>()
    val soapService = OnlineBatchReceiverSoapImpl(topicService, kafkaProducer)
    val simpleBatch = Utils.readToString("/data/basic_data_batch.xml")

    whenever(metadataFuture.get()).thenReturn(mock())
    whenever(kafkaProducer.send(any())).thenReturn(metadataFuture)

    given("missing topic routing") {
        on("receiveOnlineBatchExternalAttachment") {
            whenever(topicService.getTopic(any(), any())).thenReturn(null)
            val result = soapService.receiveOnlineBatchExternalAttachment("username", "password",
                    "123uhjoas", 0, simpleBatch, ByteArray(0))
            it("should return FAILED_DO_NOT_RETRY") {
                result shouldBe "FAILED_DO_NOT_RETRY"
            }
        }
    }

    given("valid topic routing") {
        on("receiveOnlineBatchExternalAttachment") {
            whenever(topicService.getTopic(any(), any())).thenReturn("test")
            val result = soapService.receiveOnlineBatchExternalAttachment("username", "password",
                        "123uhjoas", 0, simpleBatch, ByteArray(0))
            it("should return OK") {
                result shouldBe "OK"
            }
        }
    }
})
