package no.nav.altinnkanal

import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.PublicMetricsAutoConfiguration
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ComponentScan

@Configuration
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
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
@ComponentScan
open class BootstrapROBEA

fun main(args: Array<String>) {
    DefaultExports.initialize()
    SpringApplication.run(BootstrapROBEA::class.java, *args)
}
