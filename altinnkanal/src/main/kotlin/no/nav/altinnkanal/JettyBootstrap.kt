package no.nav.altinnkanal

import io.prometheus.client.exporter.MetricsServlet
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.config.topicRouting
import no.nav.altinnkanal.rest.HealthCheckRestController
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.LdapUntValidator
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.apache.cxf.BusFactory
import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet
import org.apache.cxf.jaxws.EndpointImpl
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.util.Properties
import javax.xml.ws.Endpoint
import kotlin.reflect.jvm.jvmName

fun main(args: Array<String>) {

    val topicRouting = topicRouting()
    val topicService = TopicService(topicRouting)

    val kafkaProperties = Properties().apply {
        load(OnlineBatchReceiverSoapImpl::class.java.getResourceAsStream("/kafka.properties"))
        val username = System.getenv("SRVALTINNKANAL_USERNAME")
        val password = System.getenv("SRVALTINNKANAL_PASSWORD")
        setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"$username\" password=\"$password\";")
    }
    val kafkaProducer = KafkaProducer<String, ExternalAttachment>(kafkaProperties)

    val batchReceiver = OnlineBatchReceiverSoapImpl(topicService, kafkaProducer)

    val server = Server(8080)

    val inProps = java.util.HashMap<String, Any>().apply {
        put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN)
        put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT)
    }

    val jaxWsProps = HashMap<String, Any>().apply {
        put(SecurityConstants.USERNAME_TOKEN_VALIDATOR, LdapUntValidator::class.jvmName)
    }

    bootstrap(server, batchReceiver, inProps, jaxWsProps)
    server.join()
}

fun bootstrap(server: Server, batchReceiver: OnlineBatchReceiverSoap,
              interceptorProps: HashMap<String, Any> = HashMap(),
              jaxWsProps: HashMap<String, Any> = HashMap()) {
    // Configure Jax WS
    val cxfServlet = CXFNonSpringServlet()

    // Configure Jax RS
    val jaxRSSingletons = setOf<Any>(HealthCheckRestController())
    val cxfRSServlet = CXFNonSpringJaxrsServlet(jaxRSSingletons)

    // Set up servlets
    val contextHandler = ServletContextHandler().apply {
        addServlet(ServletHolder(MetricsServlet()), "/prometheus")
        addServlet(ServletHolder(cxfServlet), "/webservices/*")
        addServlet(ServletHolder(cxfRSServlet), "/*")
    }
    server.run {
        handler = contextHandler
        start()
    }
    BusFactory.setDefaultBus(cxfServlet.bus)

    val endpoint = Endpoint.publish("/OnlineBatchReceiverSoap", batchReceiver) as EndpointImpl
    if (interceptorProps.isNotEmpty()) endpoint.server.endpoint.inInterceptors.add(WSS4JInInterceptor(interceptorProps as Map<String, Any>?))
    endpoint.properties = jaxWsProps
}
