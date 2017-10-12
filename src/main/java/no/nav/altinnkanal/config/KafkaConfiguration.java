package no.nav.altinnkanal.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaConfiguration {
    @Bean
    public Producer<String, byte[]> producer() throws Exception {
        // Read kafka config
        Properties kafkaProperties = new Properties();
        kafkaProperties.load(getClass().getResourceAsStream("/kafka.properties"));
        return new KafkaProducer<>(kafkaProperties);
    }
}
