package no.nav.altinnkanal.soap;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.RoutingStatus;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.InfluxService;
import no.nav.altinnkanal.services.KafkaService;
import no.nav.altinnkanal.services.TopicService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Base64;

@Service
public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final Logger logger = LogManager.getLogger("AltinnKanal");

    private final DocumentBuilder documentBuilder;
    private final TopicService topicService;
    private final KafkaService kafkaService;
    private final InfluxService influxService;

    @Autowired
    public OnlineBatchReceiverSoapImpl(TopicService topicService, KafkaService kafkaService, InfluxService influxService) throws Exception { // TODO: better handling of exceptions
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.topicService = topicService;
        this.kafkaService = kafkaService;
        this.influxService = influxService;
    }

    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String dataBatch, byte[] attachments) {
        String serviceCode = null;
        String serviceEditionCode = null;

        try {
            long start = System.currentTimeMillis();
            ExternalAttachment externalAttachment = toAvroObject(dataBatch);

            serviceCode = externalAttachment.getSc().toString();
            serviceEditionCode = externalAttachment.getSec().toString();

            TopicMapping topicMapping = topicService.getTopicMapping(serviceCode, serviceEditionCode);

            if (topicMapping == null || !topicMapping.isEnabled()) {
                influxService.logKafkaPublishStatus(externalAttachment.getSc().toString(), externalAttachment.getSec().toString(),
                        topicMapping == null ? RoutingStatus.FAILED_MISSING : RoutingStatus.FAILED_DISABLED);
                return "FAILED_DO_NOT_RETRY";
            }

            // TODO: Validate/check if received metadata matches sent record?
            RecordMetadata metadata = kafkaService.publish(topicMapping.getTopic(), externalAttachment).get();

            long publishTime = System.currentTimeMillis() - start;
            influxService.logKafkaPublishTime(publishTime, metadata.serializedValueSize());
            influxService.logKafkaPublishStatus(serviceCode, serviceEditionCode, RoutingStatus.SUCCESS);

            return "OK";
        } catch (Exception e) {
            logger.error("Failed to send a ROBEA request to Kafka", e);
            influxService.logKafkaPublishStatus(serviceCode, serviceEditionCode, RoutingStatus.FAILED_ERROR);

            return "FAILED";
        }
    }

    public ExternalAttachment toAvroObject(String dataBatch) throws Exception {
        DocumentBuilder documentBuilder = documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        XPath xPath = XPathFactory.newInstance().newXPath();

        Document doc = documentBuilder.parse(new InputSource(new StringReader(dataBatch)));

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
