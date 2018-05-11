package no.nav.altinnkanal.soap

import io.prometheus.client.Counter
import io.prometheus.client.Summary
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
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils

class OnlineBatchReceiverSoapImpl (
    private val topicService: TopicService,
    private val kafkaProducer: Producer<String, ExternalAttachment>
) : OnlineBatchReceiverSoap {

    override fun receiveOnlineBatchExternalAttachment(username: String?, passwd: String?, receiversReference: String?, sequenceNumber: Long, dataBatch: String, attachments: ByteArray?): String {
        var serviceCode: String? = null
        var serviceEditionCode: String? = null
        var archiveReference: String? = null
        val requestLatency = requestTime.startTimer()
        var logDetails = mutableListOf(kv("SC", serviceCode), kv("SEC", serviceEditionCode),
                kv("recRef", receiversReference), kv("archRef", archiveReference), kv("seqNum", sequenceNumber))

        requestsTotal.inc()
        try {
            val externalAttachment = toAvroObject(dataBatch).also {
                serviceCode = it.getSc()
                serviceEditionCode = it.getSec()
                archiveReference = it.getArchRef()
            }

            val topic = topicService.getTopic(serviceCode!!, serviceEditionCode!!)

            logDetails = mutableListOf(kv("SC", serviceCode), kv("SEC", serviceEditionCode),
                    kv("recRef", receiversReference), kv("archRef", archiveReference), kv("seqNum", sequenceNumber))

            if (topic == null) {
                requestsFailedMissing.inc()
                logDetails.add(kv("status", "FAILED_MISSING"))
                log.warn("Denied ROBEA request due to missing/unknown codes: ${"{} ".repeat(logDetails.size)}", *logDetails.toTypedArray())
                return "FAILED_DO_NOT_RETRY"
            }

            val metadata = kafkaProducer
                    .send(ProducerRecord(topic, externalAttachment))
                    .get()

            val latency = requestLatency.observeDuration()
            requestSize.observe(metadata.serializedValueSize().toDouble())
            requestsSuccess.inc()

            logDetails.apply {
                add(kv("latency", String.format("%.0f", latency * 1000) + " ms"))
                add(kv("size", String.format("%.2f", metadata.serializedValueSize() / (1024f * 1024f)) + " MB"))
                add(kv("topic", metadata.topic()))
                add(kv("partition", metadata.partition()))
                add(kv("offset", metadata.offset()))
                add(kv("status", "SUCCESS"))
            }

            log.info("Successfully published ROBEA request to Kafka: ${"{} ".repeat(logDetails.size)}", *logDetails.toTypedArray())
            return "OK"
        } catch (e: Exception) {
            requestsFailedError.inc()
            logDetails.add(kv("status", "FAILED"))
            log.error("Failed to send a ROBEA request to Kafka: ${"{} ".repeat(logDetails.size)}", *logDetails.toTypedArray(), e)
            return "FAILED"
        }
    }

    private fun toAvroObject(dataBatch: String): ExternalAttachment {
        val reader = StringReader(StringEscapeUtils.unescapeXml(dataBatch))
        val xmlReader = xmlInputFactory.createXMLStreamReader(reader)
        var serviceCode: String? = null
        var serviceEditionCode: String? = null
        var archiveReference: String? = null

        while (xmlReader.hasNext()) {
            val eventType = xmlReader.next()
            if (eventType == XMLEvent.START_ELEMENT) {
                val tagName = xmlReader.localName
                when (tagName) {
                    "ServiceCode" -> {
                        xmlReader.next()
                        serviceCode = xmlReader.text
                    }
                    "ServiceEditionCode" -> {
                        xmlReader.next()
                        serviceEditionCode = xmlReader.text
                    }
                    "DataUnit" -> archiveReference = xmlReader.getAttributeValue(null, "archiveReference")
                }
            }
            if (archiveReference != null && serviceCode != null && serviceEditionCode != null)
                break
        }

        xmlReader.close()

        val externalAttachment = ExternalAttachment.newBuilder()
                .setSc(serviceCode)
                .setSec(serviceEditionCode)
                .setArchRef(archiveReference)
                .setBatch(dataBatch)
                .build()

        log.debug("Got a ROBEA request", externalAttachment)

        return externalAttachment
    }

    companion object {
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
    }
}
