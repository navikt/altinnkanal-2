package no.nav.altinnkanal

import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.*
import org.springframework.boot.autoconfigure.web.*
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ComponentScan

@Configuration
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
@ComponentScan
@Import(
        DispatcherServletAutoConfiguration::class,
        EmbeddedServletContainerAutoConfiguration::class,
        ServerPropertiesAutoConfiguration::class,
        PublicMetricsAutoConfiguration::class,
        CxfAutoConfiguration::class,
        EndpointAutoConfiguration::class,
        MetricRepositoryAutoConfiguration::class,
        EndpointWebMvcAutoConfiguration::class,
        ManagementServerPropertiesAutoConfiguration::class,
        WebMvcAutoConfiguration::class,
        HttpMessageConvertersAutoConfiguration::class
)
open class BootstrapROBEA

fun main(args: Array<String>) {
    DefaultExports.initialize()
    SpringApplication.run(BootstrapROBEA::class.java, *args)
}
