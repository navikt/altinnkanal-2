package no.nav.altinnkanal.soap;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.KafkaService;
import no.nav.altinnkanal.services.TopicService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Base64;

public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final Logger logger = LogManager.getLogger("AltinnKanal");

    private final DocumentBuilder documentBuilder;
    private final TopicService topicService;
    private final KafkaService kafkaService;

    public OnlineBatchReceiverSoapImpl(TopicService topicService, KafkaService kafkaService) throws Exception { // TODO: better handling of exceptions
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.topicService = topicService;
        this.kafkaService = kafkaService;
    }

    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String dataBatch, byte[] attachments) {
        try {
            ExternalAttachment externalAttachment = toAvroObject(dataBatch);

            //TopicMapping topicMapping = topicService.getTopicMapping(externalAttachment.getSc().toString(), externalAttachment.getSec().toString());
            TopicMapping topicMapping = new TopicMapping(null, null, "test", null);

            if (topicMapping == null) {
                return "FAILED_DO_NOT_RETRY";
            }

            // TODO: Validate/check if received metadata matches sent record?
            RecordMetadata metadata = kafkaService.publish(topicMapping.getTopic(), externalAttachment).get();
        } catch (Exception e) {
            logger.error("Failed to send a ROBEA request to Kafka", e);
            return "FAILED";
        }
        return "OK";
    }

    public ExternalAttachment toAvroObject(String dataBatch) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();

        Document doc = documentBuilder.parse(new InputSource(new StringReader(dataBatch)));
        //String batch = xPath.compile("//batch").evaluate(doc);
        String serviceCode = xPath.compile("//ServiceCode").evaluate(doc);
        String serviceEditionCode = xPath.compile("//ServiceEditionCode").evaluate(doc);
        String archiveReference = xPath.compile("//@archiveReference").evaluate(doc);
        String batchBase64 = base64Encoder.encodeToString(dataBatch.getBytes());

        ExternalAttachment externalAttachment = ExternalAttachment.newBuilder()
                .setBatch(batchBase64)
                .setSc(serviceCode)
                .setSec(serviceEditionCode)
                .setArchRef(archiveReference)
                .build();

        logger.debug("Got a ROBEA request", externalAttachment);

        return externalAttachment;
    }
}
