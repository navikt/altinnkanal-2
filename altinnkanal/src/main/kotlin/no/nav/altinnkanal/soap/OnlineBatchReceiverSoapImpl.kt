package no.nav.altinnkanal.soap

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import net.logstash.logback.argument.StructuredArgument
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.services.TopicService
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import java.io.StringReader

import net.logstash.logback.argument.StructuredArguments.kv
import no.altinn.webservices.ReceiveOnlineBatchExternalAttachment as Request
import no.altinn.webservices.ReceiveOnlineBatchExternalAttachmentResponse as Response

private val log = LoggerFactory.getLogger(OnlineBatchReceiverSoap::class.java.name)
private val xmlInputFactory = XMLInputFactory.newFactory()
private val requestsTotal = Counter.build()
    .name("altinnkanal_requests_total")
    .help("Total requests.").register()
private val requestsSuccess = Counter.build()
    .name("altinnkanal_requests_success")
    .help("Total successful requests.").register()
private val requestsFailedMissing = Counter.build()
    .name("altinnkanal_requests_missing")
    .help("Total failed requests due to missing/unknown SC/SEC codes.").register()
private val requestsFailedError = Counter.build()
    .name("altinnkanal_requests_error")
    .help("Total failed requests due to error.").register()
private val requestSize = Summary.build()
    .name("altinnkanal_request_size_bytes_sum").help("Request size in bytes.")
    .register()
private val requestTime = Summary.build()
    .name("altinnkanal_request_time_ms").help("Request time in milliseconds.")
    .register()

const val OK = "OK"
const val FAILED = "FAILED"
const val FAILED_DO_NOT_RETRY = "FAILED_DO_NOT_RETRY"

class OnlineBatchReceiverSoapImpl (
    private val topicService: TopicService,
    private val kafkaProducer: Producer<String, ExternalAttachment>
) : OnlineBatchReceiverSoap {
    override fun receiveOnlineBatchExternalAttachment(params: Request): Response {
        val receiversReference = params.receiversReference
        val sequenceNumber = params.sequenceNumber
        val dataBatch = params.batch ?: params.batch1 ?: throw RuntimeException("Empty batch")

        val response = Response()

        var serviceCode: String? = null
        var serviceEditionCode: String? = null
        var archiveReference: String? = null
        val requestLatency = requestTime.startTimer()
        var logDetails: MutableList<StructuredArgument>? = null

        requestsTotal.inc()
        try {
            val externalAttachment = toAvroObject(dataBatch).also {
                serviceCode = it.getServiceCode()
                serviceEditionCode = it.getServiceEditionCode()
                archiveReference = it.getArchiveReference()
            }

            logDetails = mutableListOf(kv("SC", serviceCode), kv("SEC", serviceEditionCode),
                kv("recRef", receiversReference), kv("archRef", archiveReference), kv("seqNum", sequenceNumber))

            val topic = topicService.getTopic(serviceCode!!, serviceEditionCode!!)

            if (topic == null) {
                requestsFailedMissing.inc()
                logDetails.add(kv("status", FAILED_DO_NOT_RETRY))
                log.warn("Denied ROBEA request due to missing/unknown codes: ${"{} ".repeat(logDetails.size)}", *logDetails.toTypedArray())
                response.receiveOnlineBatchExternalAttachmentResult = FAILED_DO_NOT_RETRY
                return response
            }

            val metadata = kafkaProducer
                .send(ProducerRecord(topic, externalAttachment))
                .get()

            val latency = requestLatency.observeDuration()
            requestSize.observe(metadata.serializedValueSize().toDouble())
            requestsSuccess.inc()

            logDetails.addAll(arrayOf(
                kv("latency", "${(latency * 1000).toLong()} ms"),
                kv("size", String.format("%.2f", metadata.serializedValueSize() / 1024f) + " KB"),
                kv("topic", metadata.topic()),
                kv("partition", metadata.partition()),
                kv("offset", metadata.offset()),
                kv("status", OK)
            ))
            log.info("Successfully published ROBEA request to Kafka: ${"{} ".repeat(logDetails.size)}", *logDetails.toTypedArray())
            response.receiveOnlineBatchExternalAttachmentResult = OK
            return response
        } catch (e: Exception) {
            requestsFailedError.inc()
            logDetails = logDetails ?: mutableListOf(kv("SC", serviceCode), kv("SEC", serviceEditionCode),
                kv("recRef", receiversReference), kv("archRef", archiveReference), kv("seqNum", sequenceNumber))

            logDetails.add(kv("status", FAILED))
            log.error("Failed to send a ROBEA request to Kafka: ${"{} ".repeat(logDetails.size)}", *logDetails.toTypedArray(), e)
            response.receiveOnlineBatchExternalAttachmentResult = FAILED
            return response
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
                        "ServiceCode" -> {
                            builder.serviceCode = xmlReader.elementText
                        }
                        "ServiceEditionCode" -> {
                            builder.serviceEditionCode = xmlReader.elementText
                        }
                        "DataUnit" -> {
                            builder.archiveReference = xmlReader.getAttributeValue(null, "archiveReference")
                        }
                    }
                }
            }

            builder.setBatch(dataBatch).build()
        } finally {
            xmlReader.close()
        }
    }
}
