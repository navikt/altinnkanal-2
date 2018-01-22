package no.nav.altinnkanal.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaConfiguration {

    @Bean("kafkaProperties")
    public Properties kafkaProperties() throws Exception {
        Properties kafkaProperties = new Properties();
        kafkaProperties.load(getClass().getResourceAsStream("/kafka.properties"));
        return kafkaProperties;
    }

    @Bean
    public Producer<String, Object> producer(@Qualifier("kafkaProperties") Properties kafkaProperties) {
        // Read kafka config
        return new KafkaProducer<>(kafkaProperties);
    }
}
