package no.nav.altinnkanal.batch

import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import mu.KotlinLogging
import no.nav.altinnkanal.avro.ReceivedMessage

private val logger = KotlinLogging.logger { }

class DataBatchExtractor {
    private val CDATA_START = "<![CDATA["
    private val CDATA_END = "]]>"
    private val xmlInputFactory = XMLInputFactory.newInstance()

    fun toArMessage(batch: String, callId: String): ArData {
        val xmlReader = xmlInputFactory.createXMLStreamReader(StringReader(batch))
        logger.debug { "Extracting message from data batch callid $callId" }
        val arData = ArData(callId = callId)
        try {
            while (xmlReader.hasNext()) {
                val eventType = xmlReader.next()
                if (eventType == XMLEvent.START_ELEMENT) {
                    when (xmlReader.localName) {
                        "DataUnit" -> {
                            arData.archiveReference = xmlReader
                                .getAttributeValue(null, "archiveReference")
                            arData.archiveTimeStamp = xmlReader
                                .getAttributeValue(null, "archiveTimeStamp")
                            arData.reportee = xmlReader
                                .getAttributeValue(null, "reportee")
                        }
                        "ServiceCode" -> arData.serviceCode = xmlReader.elementText
                        "ServiceEditionCode" -> arData.serviceEditionCode = xmlReader.elementText
                        "DataFormatId" -> arData.dataFormatId = xmlReader.elementText
                        "DataFormatVersion" -> arData.dataFormatVersion = xmlReader.elementText
                        "FormData" -> {
                            arData.formData = xmlReader.elementText.removePrefix(CDATA_START).removeSuffix(CDATA_END)
                            arData.numberOfForms = 1
                        }
                        "Attachment" -> {
                            val attachment = Attachment()
                            attachment.filename = xmlReader
                                .getAttributeValue(null, "fileName")
                            attachment.attachmentType = xmlReader
                                .getAttributeValue(null, "attachmentType")
                            attachment.attachmentTypeName = xmlReader
                                .getAttributeValue(null, "attachmentTypeName") ?: ""
                            attachment.encrypted = xmlReader
                                .getAttributeValue(null, "isEncrypted")!!.toBoolean()
                            attachment.dataBase64 = xmlReader.elementText
                            arData.attachments += attachment
                        }
                    }
                }
            }
            arData.numberOfAttachments = arData.attachments.size
            return arData
        } finally {
            xmlReader.close()
        }
    }

    fun toReceivedMessage(arData: ArData): ReceivedMessage {
        val attachments = mutableListOf<no.nav.altinnkanal.avro.Attachment>()
        for (attachment in arData.attachments) {
            attachments.add(
                no.nav.altinnkanal.avro.Attachment(
                    attachment.filename,
                    attachment.attachmentType,
                    attachment.attachmentTypeName,
                    attachment.encrypted,
                    attachment.dataBase64
                )
            )
        }
        val message = ReceivedMessage(
            arData.callId,
            arData.archiveReference,
            arData.archiveTimeStamp,
            arData.serviceCode,
            arData.serviceEditionCode,
            arData.dataFormatId,
            arData.dataFormatVersion,
            arData.reportee,
            arData.formData,
            arData.metadata,
            attachments
        )
        return message
    }
}
