package no.nav.altinnkanal.soap;

import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.services.TopicService;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;

import static net.logstash.logback.marker.Markers.append;

@Service
public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    private final Logger logger = LoggerFactory.getLogger(OnlineBatchReceiverSoap.class.getName());

    private final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private final TopicService topicService;
    private final Producer<String, ExternalAttachment> kafkaProducer;

    private static final Counter requestsTotal = Counter.build()
            .name("altinnkanal_requests_total")
            .help("Total requests.").register();
    private static final Counter requestsSuccess = Counter.build()
            .name("altinnkanal_requests_success")
            .help("Total successful requests.").register();
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
    public OnlineBatchReceiverSoapImpl(TopicService topicService, Producer<String, ExternalAttachment> kafkaProducer) throws Exception { // TODO: better handling of exceptions
        this.topicService = topicService;
        this.kafkaProducer = kafkaProducer;
    }

    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String dataBatch, byte[] attachments) {
        String serviceCode = null;
        String serviceEditionCode = null;
        String archiveReference = null;
        Summary.Timer requestLatency = requestTime.startTimer();

        try {
            ExternalAttachment externalAttachment = toAvroObject(dataBatch);

            serviceCode = externalAttachment.getSc();
            serviceEditionCode = externalAttachment.getSec();
            archiveReference = externalAttachment.getArchRef();

            requestsTotal.inc();

            String topic = topicService.getTopic(serviceCode, serviceEditionCode);

            if (topic == null) {
                    requestsFailedMissing.inc();
                    logger.warn(append("service_code", serviceCode)
                                .and(append("service_edition_code", serviceEditionCode))
                                .and(append("routing_status", "FAILED_MISSING"))
                                .and(append("receivers_reference", receiversReference))
                                .and(append("archive_reference", archiveReference))
                                .and(append("sequence_number", sequenceNumber)),
                            "Denied ROBEA request due to missing/unknown codes: SC={}, SEC={}, recRef={}, archRef={}, seqNum={}",
                            serviceCode, serviceEditionCode, receiversReference, archiveReference, sequenceNumber);
                return "FAILED_DO_NOT_RETRY";
            }

            RecordMetadata metadata = kafkaProducer.send(new ProducerRecord<>(topic, externalAttachment)).get();

            double latency = requestLatency.observeDuration();
            requestSize.observe(metadata.serializedValueSize());
            requestsSuccess.inc();

            String requestLatencyString = String.format("%.0f", latency * 1000) + " ms";
            String requestSizeString = String.format("%.2f", metadata.serializedValueSize() / 1024f) + " MB";

            logger.info(append("service_code", serviceCode).and(append("service_edition_code", serviceEditionCode))
                        .and(append("latency", requestLatencyString))
                        .and(append("size", requestSizeString))
                        .and(append("routing_status", "SUCCESS"))
                        .and(append("receivers_reference", receiversReference))
                        .and(append("archive_reference", archiveReference))
                        .and(append("sequence_number", sequenceNumber))
                        .and(append("kafka_topic", topic)),
                    "Successfully published ROBEA request to Kafka: SC={}, SEC={}, latency={}, size={}, recRef={}, archRef={}, seqNum={}, topic={}",
                    serviceCode, serviceEditionCode, requestLatencyString, requestSizeString, receiversReference, archiveReference, sequenceNumber, topic);
            return "OK";
        } catch (Exception e) {
            requestsFailedError.inc();
            logger.error(append("service_code", serviceCode)
                        .and(append("service_edition_code", serviceEditionCode))
                        .and(append("routing_status", "FAILED"))
                        .and(append("receivers_reference", receiversReference))
                        .and(append("archive_reference", archiveReference))
                        .and(append("sequence_number", sequenceNumber)),
                    "Failed to send a ROBEA request to Kafka: SC={}, SEC={}, recRef={}, archRef={}, seqNum={}",
                    serviceCode, serviceEditionCode, receiversReference, archiveReference, sequenceNumber, e);
            return "FAILED";
        }
    }

    private ExternalAttachment toAvroObject(String dataBatch) throws Exception {
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

        ExternalAttachment externalAttachment = ExternalAttachment.newBuilder()
                .setSc(serviceCode)
                .setSec(serviceEditionCode)
                .setArchRef(archiveReference)
                .setBatch(dataBatch)
                .build();

        logger.debug("Got a ROBEA request", externalAttachment);

        return externalAttachment;
    }
}
