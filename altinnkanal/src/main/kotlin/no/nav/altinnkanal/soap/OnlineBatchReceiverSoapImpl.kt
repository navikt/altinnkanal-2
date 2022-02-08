package no.nav.altinnkanal.soap

import java.io.StringReader
import java.util.UUID
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import mu.KotlinLogging
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.kv
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.Metrics
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.avro.ReceivedMessage
import no.nav.altinnkanal.batch.DataBatchExtractor
import no.nav.altinnkanal.metadata.XmlMetaData
import no.nav.altinnkanal.services.AivenTopiccService
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.services.TopicService2
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

enum class Status {
    OK, FAILED, FAILED_DO_NOT_RETRY
}

private val log = KotlinLogging.logger { }
private val xmlInputFactory = XMLInputFactory.newInstance()

class OnlineBatchReceiverSoapImpl(
    private val topicService: TopicService,
    private val kafkaProducer: Producer<String, ExternalAttachment>,
    private val producer: Producer<String, ReceivedMessage>,
    private val aivenProducer: Producer<String, ReceivedMessage>,
) : OnlineBatchReceiverSoap {
    private val topicService2 = TopicService2()
    private val aivenService = AivenTopiccService()
    private val dataBatchExtractor = DataBatchExtractor()
    private val xmlMetaData = XmlMetaData()

    override fun receiveOnlineBatchExternalAttachment(
        username: String?,
        passwd: String?,
        receiversReference: String?,
        sequenceNumber: Long,
        dataBatch: String,
        attachments: ByteArray?
    ): String {

        val callId = UUID.randomUUID().toString()
        var serviceCode: String? = null
        var serviceEditionCode: String? = null
        var archiveReference: String? = null
        val requestLatency = Metrics.requestTime.startTimer()
        var logDetails: MutableList<StructuredArgument>? = null

        Metrics.requestsTotal.inc()
        try {
            val externalAttachment = dataBatch.toAvroObject(callId).also {
                serviceCode = it.getServiceCode()
                serviceEditionCode = it.getServiceEditionCode()
                archiveReference = it.getArchiveReference()
            }

            logDetails = mutableListOf(
                kv("callId", callId), kv("SC", serviceCode), kv("SEC", serviceEditionCode),
                kv("recRef", receiversReference), kv("archRef", archiveReference), kv("seqNum", sequenceNumber)
            )

            val topics = topicService.getTopics(serviceCode!!, serviceEditionCode!!)
            val topics2 = topicService2.getTopics(serviceCode!!, serviceEditionCode!!)
            val aivenTopics = aivenService.getTopics(serviceCode!!, serviceEditionCode!!)
            if (topics == null && topics2 == null && aivenTopics == null) {
                Metrics.requestsFailedMissing.inc()
                with(Status.FAILED_DO_NOT_RETRY) {
                    log(logDetails!!)
                    return receiptResponse(archiveReference)
                }
            }
            topics?.forEach { topic ->
                val metadata = kafkaProducer
                    .send(ProducerRecord(topic, externalAttachment))
                    .get()

                val latency = requestLatency.observeDuration()
                Metrics.requestSize.observe(metadata.serializedValueSize().toDouble())

                val logDetailsCopy = logDetails!!.toMutableList()
                logDetailsCopy.addAll(
                    arrayOf(
                        kv("latency", "${(latency * 1000).toLong()} ms"),
                        kv("size", String.format("%.2f", metadata.serializedValueSize() / 1024f) + " KB"),
                        kv("topic", metadata.topic()),
                        kv("partition", metadata.partition()),
                        kv("offset", metadata.offset())
                    )
                )
                Status.OK.log(logDetailsCopy)
            }
            topics2?.forEach { topic2 ->
                val rm = dataBatchExtractor.toReceivedMessage(externalAttachment.getBatch(), callId)
                val metadata = producer
                    .send(ProducerRecord(topic2, rm))
                    .get()

                val latency = requestLatency.observeDuration()
                Metrics.requestSize.observe(metadata.serializedValueSize().toDouble())

                val logDetailsCopy = logDetails!!.toMutableList()
                logDetailsCopy.addAll(
                    arrayOf(
                        kv("latency", "${(latency * 1000).toLong()} ms"),
                        kv("size", String.format("%.2f", metadata.serializedValueSize() / 1024f) + " KB"),
                        kv("topic", metadata.topic()),
                        kv("partition", metadata.partition()),
                        kv("offset", metadata.offset())
                    )
                )
                Status.OK.log(logDetailsCopy)
            }
            aivenTopics?.forEach { aivenTopic ->
                val rm = dataBatchExtractor.toReceivedMessage(externalAttachment.getBatch(), callId)
                val metadataConfig = aivenService.getMetaData(rm.getServiceCode(), rm.getServiceEditionCode())
                if (metadataConfig.isNotEmpty()) {
                    log.debug { "Found metadata config on service ${rm.getServiceCode()}/${rm.getServiceEditionCode()}" }
                    xmlMetaData.extractDataFromMessage(rm, metadataConfig)
                }
                val metadata = aivenProducer
                    .send(ProducerRecord(aivenTopic, rm))
                    .get()

                val latency = requestLatency.observeDuration()
                Metrics.requestSize.observe(metadata.serializedValueSize().toDouble())

                val logDetailsCopy = logDetails!!.toMutableList()
                logDetailsCopy.addAll(
                    arrayOf(
                        kv("latency", "${(latency * 1000).toLong()} ms"),
                        kv("size", String.format("%.2f", metadata.serializedValueSize() / 1024f) + " KB"),
                        kv("topic", metadata.topic()),
                        kv("partition", metadata.partition()),
                        kv("offset", metadata.offset())
                    )
                )
                Status.OK.log(logDetailsCopy)
            }
            Metrics.requestsSuccess.labels("$serviceCode", "$serviceEditionCode").inc()
            return Status.OK.receiptResponse(archiveReference)
        } catch (e: Exception) {
            Metrics.requestsFailedError.inc()
            logDetails = logDetails ?: mutableListOf(
                kv("callId", callId), kv("SC", serviceCode), kv("SEC", serviceEditionCode),
                kv("recRef", receiversReference), kv("archRef", archiveReference), kv("seqNum", sequenceNumber)
            )
            with(Status.FAILED) {
                log(logDetails, e)
                return receiptResponse(archiveReference, e)
            }
        }
    }
}

private fun String.toAvroObject(callId: String): ExternalAttachment {
    val xmlReader = xmlInputFactory.createXMLStreamReader(StringReader(this))
    return try {
        val builder = ExternalAttachment.newBuilder()
        while (xmlReader.hasNext() && (!builder.hasArchiveReference() || !builder.hasServiceCode() || !builder.hasServiceEditionCode())) {
            val eventType = xmlReader.next()
            if (eventType == XMLEvent.START_ELEMENT) {
                when (xmlReader.localName) {
                    "ServiceCode" -> builder.serviceCode = xmlReader.elementText
                    "ServiceEditionCode" -> builder.serviceEditionCode = xmlReader.elementText
                    "DataUnit" -> builder.archiveReference = xmlReader.getAttributeValue(null, "archiveReference")
                }
            }
        }
        builder.callId = callId
        builder.setBatch(this).build()
    } finally {
        xmlReader.close()
    }
}

private fun Status.log(logDetails: MutableList<StructuredArgument>, e: Exception? = null) {
    logDetails.add(kv("status", this))
    val (logString, logArray) = logDetails.joinToString { "{}" } to logDetails.toTypedArray()
    when (this) {
        Status.OK -> log.info("Successfully published ROBEA request to Kafka: $logString", *logArray)
        Status.FAILED_DO_NOT_RETRY -> log.warn("Denied ROBEA request due to missing/unknown codes: $logString", *logArray)
        Status.FAILED -> log.error("Failed to send a ROBEA request to Kafka: $logString", *logArray, e)
    }
}

private fun Status.receiptResponse(archRef: String?, e: Exception? = null): String {
    val message: String = when (this) {
        Status.OK -> "Message received OK (archiveReference=$archRef)"
        Status.FAILED_DO_NOT_RETRY -> "Invalid combination of Service Code and Service Edition Code (archiveReference=$archRef)"
        Status.FAILED -> "An error occurred: ${e?.message} (archiveReference=$archRef)"
    }
    return "&lt;OnlineBatchReceipt&gt;&lt;Result resultCode=&quot;$this&quot;&gt;$message&lt;/Result&gt;&lt;/OnlineBatchReceipt&gt;"
}
