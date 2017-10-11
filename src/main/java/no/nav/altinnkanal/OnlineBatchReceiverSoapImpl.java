package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.TopicService;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;

public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    private final Base64.Encoder base64Decoder = Base64.getEncoder();
    private final Logger logger = LogManager.getLogger("AltinnKanal");

    private final DocumentBuilder documentBuilder;
    private final Producer<String, byte[]> producer;
    private final TopicService topicService;

    public OnlineBatchReceiverSoapImpl(Producer<String, byte[]> producer, TopicService topicService) throws Exception { // TODO: better handling of exceptions
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.producer = producer;
        this.topicService = topicService;
    }

    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String dataBatch, byte[] attachments) {
        try {
            ExternalAttachment externalAttachment = toAvroObject(dataBatch);

            TopicMapping topic = topicService.getTopic(externalAttachment.getSc().toString(), externalAttachment.getSec().toString());

            if (topic == null) {
                return "FAILED_DO_NOT_RETRY";
            }

            producer.send(new ProducerRecord<>("test", encodeAvroObject(externalAttachment, ExternalAttachment.class))).get();
        } catch (Exception e) {
            logger.error("Failed to send a ROBEA request to Kafka", e);
            return "FAILED";
        }
        return "OK";
    }

    private <T> byte[] encodeAvroObject(T avroObject, Class<T> cl) throws IOException {
        DatumWriter<T> datumWriter = new SpecificDatumWriter<>(cl);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(byteOut, null);
        datumWriter.write(avroObject, encoder);
        encoder.flush();
        byteOut.close();
        return byteOut.toByteArray();
    }

    public ExternalAttachment toAvroObject(String dataBatch) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();

        Document doc = documentBuilder.parse(new InputSource(new StringReader(dataBatch)));
        //String batch = xPath.compile("//batch").evaluate(doc);
        String serviceCode = xPath.compile("//ServiceCode").evaluate(doc);
        String serviceEditionCode = xPath.compile("//ServiceEditionCode").evaluate(doc);
        String archiveReference = xPath.compile("//@archiveReference").evaluate(doc);
        String batchBase64 = base64Decoder.encodeToString(dataBatch.getBytes());

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
