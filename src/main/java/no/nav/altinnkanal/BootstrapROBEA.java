package no.nav.altinnkanal;

import javax.xml.ws.Endpoint;

public class BootstrapROBEA {

    public static void main(String[] args) {
        new BootstrapROBEA().start();
    }

    public void start() {
        OnlineBatchReceiverSoapImpl onlineBatchReceiverSoap = new OnlineBatchReceiverSoapImpl();
        String address = "http://localhost:8080/altinnkanal/OnlineBatchReceiverSoap";
        Endpoint.publish(address, onlineBatchReceiverSoap);

        try {
            Thread.sleep(5*60*100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
