package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    private final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final DocumentBuilder documentBuilder;

    public OnlineBatchReceiverSoapImpl() throws Exception { // TODO: better handling of exceptions
        documentBuilder = documentBuilderFactory.newDocumentBuilder();
    }

    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String dataBatch, byte[] attachments) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Document doc = documentBuilder.parse(new InputSource(new StringReader(dataBatch)));
            String batch = xPath.compile("//batch").evaluate(doc);
            String serviceCode = xPath.compile("//ServiceCode").evaluate(doc);
            String serviceEditionCode = xPath.compile("//ServiceEditionCode").evaluate(doc);
            String archRef = xPath.compile("//@archiveReference").evaluate(doc);
            System.out.println("SC: " + serviceCode);
            System.out.println("SEC: " + serviceEditionCode);
            System.out.println("archRef: " + archRef);
            System.out.println("batch: " + batch);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: log
            return "FAILED";
        }
        System.out.println(dataBatch);
        return "OK";
    }
}
