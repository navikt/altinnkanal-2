package no.nav.altinnkanal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Future
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.altinnkanal.soap.SoapResponse.FAILED_DO_NOT_RETRY
import no.nav.altinnkanal.soap.SoapResponse.OK
import org.amshove.kluent.shouldBe
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.RecordMetadata
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.data_driven.data
import org.jetbrains.spek.data_driven.on

object OnlineBatchReceiverSoapSpec : Spek({
    val topicService = mock<TopicService>()
    val kafkaProducer = mock<Producer<String, ExternalAttachment>>()
    val metadataFuture = mock<Future<RecordMetadata>>()
    val soapService = OnlineBatchReceiverSoapImpl(topicService, kafkaProducer)
    val simpleBatch = Utils.readToString("/data/basic_data_batch.xml")

    whenever(metadataFuture.get()).thenReturn(mock())
    whenever(kafkaProducer.send(any())).thenReturn(metadataFuture)

    describe("receiveOnlineBatchExternalAttachment") {
        on("%s",
            data<String, String?, String>("valid topic routing", "test", expected = OK),
            data<String, String?, String>("missing topic routing", null, expected = FAILED_DO_NOT_RETRY)
        ) { _, mockValue: String?, expected: String ->
            println(mockValue)
            whenever(topicService.getTopic(any(), any())).thenReturn(mockValue)
            val result = soapService.receiveOnlineBatchExternalAttachment("username", "password",
                    "123uhjoas", 0, simpleBatch, ByteArray(0))
            it("should return $expected") {
                result shouldBe expected
            }
        }
    }
})
