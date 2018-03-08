package no.nav.altinnkanal

import io.prometheus.client.exporter.MetricsServlet
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.config.SoapProperties
import no.nav.altinnkanal.config.topicRouting
import no.nav.altinnkanal.rest.HealthCheckRestController
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.altinnkanal.soap.UntPasswordCallback
import org.apache.cxf.BusFactory
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet
import org.apache.cxf.jaxws.EndpointImpl
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.util.*
import javax.xml.ws.Endpoint

fun main(args: Array<String>) {
    val soapProperties = SoapProperties()

    val topicRouting = topicRouting()
    val topicService = TopicService(topicRouting)

    val kafkaProperties = Properties()
    kafkaProperties.load(OnlineBatchReceiverSoapImpl::class.java.getResourceAsStream("/kafka.properties"))
    val kafkaProducer = KafkaProducer<String, ExternalAttachment>(kafkaProperties)

    val batchReceiver = OnlineBatchReceiverSoapImpl(topicService, kafkaProducer)

    val server = Server(8080)

    bootstrap(server, soapProperties, batchReceiver)

    server.join()
}

fun bootstrap(server: Server, soapProperties: SoapProperties, batchReceiver: OnlineBatchReceiverSoap) {
    // Configure Jax WS
    val cxfServlet = CXFNonSpringServlet()

    // Configure Jax RS
    val jaxRSSingletons = setOf<Any>(HealthCheckRestController())
    val cxfRSServlet = CXFNonSpringJaxrsServlet(jaxRSSingletons)

    // Set up servlets
    val contextHandler = ServletContextHandler()
    contextHandler.addServlet(ServletHolder(MetricsServlet()), "/prometheus")
    contextHandler.addServlet(ServletHolder(cxfServlet), "/webservices/*")
    contextHandler.addServlet(ServletHolder(cxfRSServlet), "/*")

    server.handler = contextHandler
    server.start()

    BusFactory.setDefaultBus(cxfServlet.bus)
    val endpointImpl = Endpoint.publish("/OnlineBatchReceiverSoap", batchReceiver) as EndpointImpl
    val endpoint = endpointImpl.server.endpoint

    val inProps = java.util.HashMap<String, Any>()
    inProps[WSHandlerConstants.ACTION] = WSHandlerConstants.USERNAME_TOKEN
    inProps[WSHandlerConstants.PASSWORD_TYPE] = WSConstants.PW_TEXT
    inProps[WSHandlerConstants.PW_CALLBACK_REF] = UntPasswordCallback(soapProperties)

    val wssIn = WSS4JInInterceptor(inProps as Map<String, Any>?)
    endpoint.inInterceptors.add(wssIn)
}
