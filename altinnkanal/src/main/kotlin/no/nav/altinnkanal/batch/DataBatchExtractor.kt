package no.nav.altinnkanal.batch

import java.io.StringReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import mu.KotlinLogging
import no.nav.altinnkanal.avro.Attachment
import no.nav.altinnkanal.avro.ReceivedMessage

private val logger = KotlinLogging.logger { }

class DataBatchExtractor {
    private val CDATA_START = "<![CDATA["
    private val CDATA_END = "]]>"
    private val xmlInputFactory = XMLInputFactory.newInstance()

    fun toReceivedMessage(batch: String, callId: String): ReceivedMessage {
        val xmlReader = xmlInputFactory.createXMLStreamReader(StringReader(batch))
        logger.debug { "Extracting message from data batch callid $callId" }
        val receivedMessage = ReceivedMessage()
        val attachments = mutableListOf<Attachment>()
        receivedMessage.setCallId(callId)
        receivedMessage.setMetadata(emptyMap())
        try {
            while (xmlReader.hasNext()) {
                val eventType = xmlReader.next()
                if (eventType == XMLEvent.START_ELEMENT) {
                    when (xmlReader.localName) {
                        "DataUnit" -> {
                            receivedMessage.setArchiveReference(
                                xmlReader.getAttributeValue(null, "archiveReference")
                            )
                            receivedMessage.setArchiveTimeStamp(
                                xmlReader.getAttributeValue(null, "archiveTimeStamp")
                            )
                            receivedMessage.setReportee(
                                xmlReader.getAttributeValue(null, "reportee")
                            )
                        }
                        "ServiceCode" -> receivedMessage.setServiceCode(xmlReader.elementText)
                        "ServiceEditionCode" -> receivedMessage.setServiceEditionCode(xmlReader.elementText)
                        "DataFormatId" -> receivedMessage.setDataFormatId(xmlReader.elementText)
                        "DataFormatVersion" -> receivedMessage.setDataFormatVersion(xmlReader.elementText)
                        "FormData" -> {
                            receivedMessage.setXmlMessage(xmlReader.elementText.removePrefix(CDATA_START).removeSuffix(CDATA_END))
                        }
                        "Attachment" -> {
                            val attachment = Attachment()
                            attachment.setFilename(
                                xmlReader.getAttributeValue(null, "fileName")
                            )
                            attachment.setAttachmentType(
                                xmlReader.getAttributeValue(null, "attachmentType")
                            )
                            attachment.setAttachmentTypeName(
                                xmlReader.getAttributeValue(null, "attachmentTypeName") ?: ""
                            )
                            attachment.setEncrypted(
                                xmlReader.getAttributeValue(null, "isEncrypted")!!.toBoolean()
                            )
                            attachment.setDataBase64(xmlReader.elementText)
                            attachments.add(attachment)
                        }
                    }
                }
            }
            receivedMessage.setAttachments(attachments)
            return receivedMessage
        } finally {
            xmlReader.close()
        }
    }
}
