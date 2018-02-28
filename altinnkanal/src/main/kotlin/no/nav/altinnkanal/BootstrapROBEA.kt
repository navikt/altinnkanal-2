package no.nav.altinnkanal

import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.PublicMetricsAutoConfiguration
import org.springframework.boot.autoconfigure.web.*
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration
import org.springframework.context.annotation.ComponentScan


@Configuration
@Import(
        DispatcherServletAutoConfiguration::class,
        EmbeddedServletContainerAutoConfiguration::class,
        ServerPropertiesAutoConfiguration::class,
        PublicMetricsAutoConfiguration::class,
        CxfAutoConfiguration::class
)
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
@ComponentScan
open class BootstrapROBEA

fun main(args: Array<String>) {
    DefaultExports.initialize()
    SpringApplication.run(BootstrapROBEA::class.java, *args)
}