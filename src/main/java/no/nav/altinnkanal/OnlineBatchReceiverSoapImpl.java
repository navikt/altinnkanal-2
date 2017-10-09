package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;

public class OnlineBatchReceiverSoapImpl implements OnlineBatchReceiverSoap {
    public String receiveOnlineBatchExternalAttachment(String username, String passwd, String receiversReference, long sequenceNumber, String batch, byte[] attachments) {
        return "OK";
    }
}
