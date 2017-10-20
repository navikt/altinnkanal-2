package no.nav.altinnkanal.config;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.services.InfluxService;
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
    public Endpoint endpoint(OnlineBatchReceiverSoap onlineBatchReceiverSoap, Bus bus) throws Exception {
        EndpointImpl endpoint = new EndpointImpl(bus, onlineBatchReceiverSoap);
        endpoint.publish("/OnlineBatchReceiverSoap");
        return endpoint;
    }
}
