package no.nav.altinnkanal.soap;

import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.KafkaService;
import no.nav.altinnkanal.services.TopicService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.util.Base64;

import static net.logstash.logback.marker.Markers.append;

@Service
public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final Logger logger = LoggerFactory.getLogger(OnlineBatchReceiverSoap.class.getName());

    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private final TopicService topicService;
    private final KafkaService kafkaService;

    private static final Counter requestsTotal = Counter.build()
            .name("altinnkanal_requests_total")
            .help("Total requests.").register();
    private static final Counter requestsSuccess = Counter.build()
            .name("altinnkanal_requests_success")
            .help("Total successful requests.").register();
    private static final Counter requestsFailedDisabled = Counter.build()
            .name("altinnkanal_requests_disabled")
            .help("Total failed requests due to disabled SC/SEC codes.").register();
    private static final Counter requestsFailedMissing = Counter.build()
            .name("altinnkanal_requests_missing")
            .help("Total failed requests due to missing/unknown SC/SEC codes.").register();
    private static final Counter requestsFailedError = Counter.build()
            .name("altinnkanal_requests_error")
            .help("Total failed requests due to error.").register();
    private static final Summary requestSize = Summary.build()
            .name("altinnkanal_request_size_bytes_sum").help("Request size in bytes.")
            .register();
    private static final Summary requestTime = Summary.build()
            .name("altinnkanal_request_time_ms").help("Request time in milliseconds.")
            .register();

    @Autowired
    public OnlineBatchReceiverSoapImpl(TopicService topicService, KafkaService kafkaService) throws Exception { // TODO: better handling of exceptions
        this.topicService = topicService;
        this.kafkaService = kafkaService;
    }

    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String dataBatch, byte[] attachments) {
        String serviceCode = null;
        String serviceEditionCode = null;
        Summary.Timer requestLatency = requestTime.startTimer();

        try {
            ExternalAttachment externalAttachment = toAvroObject(dataBatch);

            serviceCode = externalAttachment.getSc();
            serviceEditionCode = externalAttachment.getSec();

            requestsTotal.inc();

            TopicMapping topicMapping = topicService.getTopicMapping(serviceCode, serviceEditionCode);

            if (topicMapping == null || !topicMapping.isEnabled()) {
                if (topicMapping == null) {
                    requestsFailedMissing.inc();
                    logger.warn(append("service_code", serviceCode).and(append("service_edition_code", serviceEditionCode)),
                            "Denied ROBEA request due to missing/unknown codes: SC={}, SEC={}", serviceCode, serviceEditionCode);
                }
                else {
                    requestsFailedDisabled.inc();
                    logger.warn(append("service_code", serviceCode).and(append("service_edition_code", serviceEditionCode)),
                            "Denied ROBEA request due to disabled codes: SC={}, SEC={}", serviceCode, serviceEditionCode);
                }
                return "FAILED_DO_NOT_RETRY";
            }

            RecordMetadata metadata = kafkaService.publish(topicMapping.getTopic(), externalAttachment).get();

            double latency = requestLatency.observeDuration();
            requestSize.observe(metadata.serializedValueSize());
            requestsSuccess.inc();

            String requestLatencyString = String.format("%.0f", latency * 1000) + " ms";
            String requestSizeString = String.format("%.2f", metadata.serializedValueSize() / 1024f) + " MB";

            logger.info(append("service_code", serviceCode).and(append("service_edition_code", serviceEditionCode))
                        .and(append("latency", requestLatencyString))
                        .and(append("size", requestSizeString)),
                    "Successfully published ROBEA request to Kafka: SC={}, SEC={}, latency={}, size={})",
                    serviceCode, serviceEditionCode, requestLatencyString, requestSizeString);
            return "OK";
        } catch (Exception e) {
            logger.error(append("service_code", serviceCode).and(append("service_edition_code", serviceEditionCode)),
                    "Failed to send a ROBEA request to Kafka: SC={}, SEC={}", serviceCode, serviceEditionCode, e);

            requestsFailedError.inc();

            return "FAILED";
        }
    }

    public ExternalAttachment toAvroObject(String dataBatch) throws Exception {
        StringReader reader = new StringReader(dataBatch);
        XMLStreamReader xmlReader = xmlInputFactory.createXMLStreamReader(reader);
        String serviceCode = null;
        String serviceEditionCode = null;
        String archiveReference = null;

        while (xmlReader.hasNext()) {
            int eventType = xmlReader.next();
            if (eventType == XMLEvent.START_ELEMENT) {
                String tagName = xmlReader.getLocalName();
                switch (tagName) {
                    case "ServiceCode":
                        xmlReader.next();
                        serviceCode = xmlReader.getText();
                        break;
                    case "ServiceEditionCode":
                        xmlReader.next();
                        serviceEditionCode = xmlReader.getText();
                        break;
                    case "DataUnit":
                        archiveReference = xmlReader.getAttributeValue(null, "archiveReference");
                        break;
                }
            }
            if (archiveReference != null && serviceCode != null && serviceEditionCode != null)
                break;
        }

        xmlReader.close();

        String batchBase64 = base64Encoder.encodeToString(dataBatch.getBytes());

        ExternalAttachment externalAttachment = ExternalAttachment.newBuilder()
                .setSc(serviceCode)
                .setSec(serviceEditionCode)
                .setArchRef(archiveReference)
                .setBatch(batchBase64)
                .build();

        logger.debug("Got a ROBEA request", externalAttachment);

        return externalAttachment;
    }
}
