package no.nav.altinnkanal.config;

import no.nav.altinnkanal.OnlineBatchReceiverSoapImpl;
import no.nav.altinnkanal.services.TopicService;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;

@Configuration
public class SoapConfiguration {

    @Bean
    public Endpoint endpoint(Producer<String, byte[]> producer, TopicService topicService, Bus bus) throws Exception {
        EndpointImpl endpoint = new EndpointImpl(bus, new OnlineBatchReceiverSoapImpl(producer, topicService));
        endpoint.publish("/OnlineBatchReceiverSoap");
        return endpoint;
    }
}
