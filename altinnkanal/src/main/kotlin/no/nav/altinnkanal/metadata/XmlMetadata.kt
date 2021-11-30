package no.nav.altinnkanal.metadata

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory
import mu.KotlinLogging
import no.nav.altinnkanal.avro.ReceivedMessage
import org.w3c.dom.Document
import org.xml.sax.InputSource

private val logger = KotlinLogging.logger { }

open class XmlMetaData {

    open fun extractDataFromMessage(message: ReceivedMessage, metaData: Map<String, String>): Boolean {
        try {
            val xmlMessage = message.getXmlMessage()
            val xPath = XPathFactory.newInstance().newXPath()

            val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc: Document = documentBuilder.parse(InputSource(xmlMessage.reader()))
            val metadata = mutableMapOf<String, String>()
            metaData.forEach {
                logger.info { "Extracting data on service ${message.getServiceCode()}/${message.getServiceEditionCode()} to metadata for path $it" }
                val xpath = it.value
                if (xpath.contains("/")) {
                    metadata[it.key] = xPath.evaluate("/$xpath/text()", doc)
                } else {
                    metadata[it.key] = xPath.evaluate("//*[local-name()='$xpath']/text()", doc)
                }
            }
            message.setMetadata(metadata)
            return true
        } catch (e: Exception) {
            logger.warn { "Failed to extract info from xml message ${message.getArchiveReference()}, SC=${message.getServiceCode()}, SEC=${message.getServiceEditionCode()}" }
        }
        return false
    }
}
