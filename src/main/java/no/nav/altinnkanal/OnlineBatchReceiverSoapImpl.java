package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
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
import java.io.StringReader;
import java.util.Base64;

public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    private final DocumentBuilder documentBuilder;
    private final Base64.Encoder base64Decoder = Base64.getEncoder();
    private final Producer<String, byte[]> producer;
    private final Logger logger = LogManager.getLogger("AltinnKanal");

    public OnlineBatchReceiverSoapImpl(Producer<String, byte[]> producer) throws Exception { // TODO: better handling of exceptions
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
        this.producer = producer;
    }

    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String dataBatch, byte[] attachments) {
        DatumWriter<ExternalAttachment> datumWriter = new SpecificDatumWriter<>(ExternalAttachment.class);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try {
            Encoder encoder = EncoderFactory.get().binaryEncoder(byteOut, null);
            ExternalAttachment externalAttachment = toAvroObject(dataBatch);
            datumWriter.write(externalAttachment, encoder);
            encoder.flush();
            byteOut.close();
            producer.send(new ProducerRecord<>("test", byteOut.toByteArray())).get();
        } catch (Exception e) {
            logger.error("Failed to send a ROBEA request to Kafka", e);
            return "FAILED";
        }
        return "OK";
    }

    public ExternalAttachment toAvroObject(String dataBatch) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();

        Document doc = documentBuilder.parse(new InputSource(new StringReader(dataBatch)));
        String batch = xPath.compile("//batch").evaluate(doc);
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
