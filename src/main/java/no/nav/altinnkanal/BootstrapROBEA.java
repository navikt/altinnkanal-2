package no.nav.altinnkanal;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;
import no.nav.integrasjon.EnvironmentTransformer;
import no.nav.integrasjon.Transformers;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
public class BootstrapROBEA {

    public static void main(String[] args) {
        EnvironmentTransformer.builder()
                .transformer("LDAP_URL", Transformers.LDAP_TRANSFORMER)
                .build()
                .mergeToSystemProperties();
        SpringApplication.run(BootstrapROBEA.class, args);
    }
}
