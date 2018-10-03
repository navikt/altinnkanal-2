package no.nav.altinnkanal.soap

import mu.KotlinLogging
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.Metrics
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.MDC
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import java.io.StringReader

enum class Status {
    OK, FAILED, FAILED_DO_NOT_RETRY
}

private val log = KotlinLogging.logger { }
private val xmlInputFactory = XMLInputFactory.newFactory()

class OnlineBatchReceiverSoapImpl(
    private val topicService: TopicService,
    private val kafkaProducer: Producer<String, ExternalAttachment>
) : OnlineBatchReceiverSoap {
    override fun receiveOnlineBatchExternalAttachment(
        username: String?,
        passwd: String?,
        receiversReference: String?,
        sequenceNumber: Long,
        dataBatch: String,
        attachments: ByteArray?
    ): String {

        var serviceCode: String? = null
        var serviceEditionCode: String? = null
        var archiveReference: String? = null
        val requestLatency = Metrics.requestTime.startTimer()

        Metrics.requestsTotal.inc()
        try {
            val externalAttachment = toAvroObject(dataBatch).also {
                serviceCode = it.getServiceCode()
                serviceEditionCode = it.getServiceEditionCode()
                archiveReference = it.getArchiveReference()
            }

            MDC.put("SC", serviceCode)
            MDC.put("SEC", serviceEditionCode)
            MDC.put("recRef", receiversReference)
            MDC.put("archRef", archiveReference)
            MDC.put("seqNum", sequenceNumber.toString())

            val topic = topicService.getTopic(serviceCode!!, serviceEditionCode!!)

            if (topic == null) {
                Metrics.requestsFailedMissing.inc()
                MDC.put("status", Status.FAILED_DO_NOT_RETRY.name)
                return receiptResponse(Status.FAILED_DO_NOT_RETRY, archiveReference)
            }

            val metadata = kafkaProducer
                .send(ProducerRecord(topic, externalAttachment))
                .get()

            val latency = requestLatency.observeDuration()
            Metrics.requestSize.observe(metadata.serializedValueSize().toDouble())
            Metrics.requestsSuccess.labels("$serviceCode", "$serviceEditionCode").inc()

            MDC.put("latency", "${(latency * 1000).toLong()} ms")
            MDC.put("size", String.format("%.2f", metadata.serializedValueSize() / 1024f) + " KB")
            MDC.put("topic", metadata.topic())
            MDC.put("partition", metadata.partition().toString())
            MDC.put("offset", metadata.offset().toString())
            MDC.put("status", Status.OK.name)
            return receiptResponse(Status.OK, archiveReference)
        } catch (e: Exception) {
            Metrics.requestsFailedError.inc()
            MDC.put("SC", serviceCode)
            MDC.put("SEC", serviceEditionCode)
            MDC.put("recRef", receiversReference)
            MDC.put("archRef", archiveReference)
            MDC.put("seqNum", sequenceNumber.toString())
            MDC.put("status", Status.FAILED.name)
            return receiptResponse(Status.FAILED, archiveReference, e)
        } finally {
            MDC.clear()
        }
    }

    private fun toAvroObject(dataBatch: String): ExternalAttachment {
        val xmlReader = xmlInputFactory.createXMLStreamReader(StringReader(dataBatch))
        return try {
            val builder = ExternalAttachment.newBuilder()
            while (xmlReader.hasNext() && (!builder.hasArchiveReference() || !builder.hasServiceCode() || !builder.hasServiceEditionCode())) {
                val eventType = xmlReader.next()
                if (eventType == XMLEvent.START_ELEMENT) {
                    when (xmlReader.localName) {
                        "ServiceCode" -> builder.serviceCode = xmlReader.elementText
                        "ServiceEditionCode" -> builder.serviceEditionCode = xmlReader.elementText
                        "DataUnit" -> builder.archiveReference = xmlReader
                            .getAttributeValue(null, "archiveReference")
                    }
                }
            }
            builder.callId = MDC.get("callId")
            builder.setBatch(dataBatch).build()
        } finally {
            xmlReader.close()
        }
    }

    private fun receiptResponse(
        resultCode: Status,
        archRef: String?,
        e: Exception? = null
    ): String {
        val message: String = when (resultCode) {
            Status.OK -> {
                log.info("Successfully published ROBEA request to Kafka")
                "Message received OK (archiveReference=$archRef)"
            }
            Status.FAILED_DO_NOT_RETRY -> {
                log.warn("Denied ROBEA request due to missing/unknown codes")
                "Invalid combination of Service Code and Service Edition Code (archiveReference=$archRef)"
            }
            Status.FAILED -> {
                log.error("Failed to send a ROBEA request to Kafka", e)
                "An error occurred: ${e?.message} (archiveReference=$archRef)"
            }
        }
        return "&lt;OnlineBatchReceipt&gt;&lt;Result resultCode=&quot;$resultCode&quot;&gt;$message&lt;/Result&gt;&lt;/OnlineBatchReceipt&gt;"
    }
}
