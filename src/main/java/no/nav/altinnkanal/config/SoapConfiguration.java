package no.nav.altinnkanal.config;

import no.nav.altinnkanal.services.KafkaService;
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl;
import no.nav.altinnkanal.services.TopicService;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.ws.Endpoint;

@Configuration
public class SoapConfiguration {

    @Bean
    public Endpoint endpoint(KafkaService kafkaService, TopicService topicService, Bus bus) throws Exception {
        EndpointImpl endpoint = new EndpointImpl(bus, new OnlineBatchReceiverSoapImpl(topicService, kafkaService));
        endpoint.publish("/OnlineBatchReceiverSoap");
        return endpoint;
    }
}
