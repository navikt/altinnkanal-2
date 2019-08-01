package no.nav.altinnkanal

import io.prometheus.client.exporter.MetricsServlet
import io.prometheus.client.hotspot.DefaultExports
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.config.KafkaConfig
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.StsUntValidator
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import org.apache.cxf.BusFactory
import org.apache.cxf.jaxws.EndpointImpl
import org.apache.cxf.transport.servlet.CXFNonSpringServlet
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import javax.xml.ws.Endpoint
import kotlin.reflect.jvm.jvmName

fun main() {
    Server(8080).run {
        bootstrap(
            this, OnlineBatchReceiverSoapImpl(
                TopicService(), KafkaProducer(KafkaConfig.config)
            )
        )
        join()
    }
}

fun bootstrap(server: Server, batchReceiver: OnlineBatchReceiverSoap) {
    // Initialize metrics
    Metrics

    // Configure Jax WS
    val cxfServlet = CXFNonSpringServlet().apply {
        bus = BusFactory.getDefaultBus(true)
    }

    server.run {
        handler = HandlerCollection().apply {
            handlers = arrayOf(
                ContextHandler("/internal").apply {
                    handler = SelfTestHandler()
                },
                ServletContextHandler(ServletContextHandler.NO_SECURITY or ServletContextHandler.NO_SESSIONS).apply {
                    addServlet(ServletHolder(MetricsServlet()), "/internal/prometheus")
                    addServlet(ServletHolder(cxfServlet), "/webservices/*")
                }
            )
        }
        start()
    }

    Endpoint.publish("/OnlineBatchReceiverSoap", batchReceiver).let {
        it as EndpointImpl
        it.server.endpoint.inInterceptors.add(
            WSS4JInInterceptor(
                mapOf(
                    WSHandlerConstants.ACTION to WSHandlerConstants.USERNAME_TOKEN,
                    WSHandlerConstants.PASSWORD_TYPE to WSConstants.PW_TEXT
                )
            )
        )
        it.properties = mapOf(SecurityConstants.USERNAME_TOKEN_VALIDATOR to StsUntValidator::class.jvmName)
    }

    DefaultExports.initialize()
}
