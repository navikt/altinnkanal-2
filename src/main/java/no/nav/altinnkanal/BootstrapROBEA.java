package no.nav.altinnkanal;

import no.nav.altinnkanal.services.TopicService;
import no.nav.altinnkanal.services.TopicServiceImpl;
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

        TopicService topicService = new TopicServiceImpl();

        OnlineBatchReceiverSoapImpl onlineBatchReceiverSoap = new OnlineBatchReceiverSoapImpl(producer, topicService);
        Endpoint.publish("http://0.0.0.0:8080/altinnkanal/OnlineBatchReceiverSoap", onlineBatchReceiverSoap);
    }
}
