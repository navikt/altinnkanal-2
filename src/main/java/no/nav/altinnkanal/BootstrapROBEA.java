package no.nav.altinnkanal;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import javax.xml.ws.Endpoint;
import java.util.Properties;

public class BootstrapROBEA {

    public static void main(String[] args) throws Exception {
        new BootstrapROBEA().start();
    }

    public void start() throws Exception {
        // Read kafka config
        Properties kafkaProperties = new Properties();
        kafkaProperties.load(getClass().getResourceAsStream("/kafka.properties"));
        Producer<String, byte[]> producer = new KafkaProducer<>(kafkaProperties);

        OnlineBatchReceiverSoapImpl onlineBatchReceiverSoap = new OnlineBatchReceiverSoapImpl(producer);
        Endpoint.publish("http://0.0.0.0:8080/altinnkanal/OnlineBatchReceiverSoap", onlineBatchReceiverSoap);
    }
}
