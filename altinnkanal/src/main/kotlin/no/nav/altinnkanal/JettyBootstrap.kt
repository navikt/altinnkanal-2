package no.nav.altinnkanal

import io.prometheus.client.exporter.MetricsServlet
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.config.SoapProperties
import no.nav.altinnkanal.config.topicRouting
import no.nav.altinnkanal.rest.HealthCheckRestController
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.apache.cxf.BusFactory
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.apache.kafka.clients.producer.KafkaProducer
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

fun bootstrap(server: Server, soapProperties: SoapProperties, batchReceiver: OnlineBatchReceiverSoapImpl) {
    // Configure Jax WS
    val cxfServlet = CXFNonSpringServlet()

    // Configure Jax RS
    val jaxRSSingletons = setOf<Any>(HealthCheckRestController(soapProperties))
    val cxfRSServlet = CXFNonSpringJaxrsServlet(jaxRSSingletons)

    // Set up servlets
    val contextHandler = ServletContextHandler()
    contextHandler.addServlet(ServletHolder(MetricsServlet()), "/prometheus")
    contextHandler.addServlet(ServletHolder(cxfServlet), "/webservices/*")
    contextHandler.addServlet(ServletHolder(cxfRSServlet), "/*")

    server.handler = contextHandler
    server.start()

    BusFactory.setDefaultBus(cxfServlet.bus)
    Endpoint.publish("/OnlineBatchReceiverSoap", batchReceiver)
}