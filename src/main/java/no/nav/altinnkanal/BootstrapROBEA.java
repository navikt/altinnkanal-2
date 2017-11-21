package no.nav.altinnkanal;

import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
public class BootstrapROBEA {

    public static void main(String[] args) {
        DefaultExports.initialize();
        SpringApplication.run(BootstrapROBEA.class, args);
    }
}
