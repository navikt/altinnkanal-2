package no.nav.altinnkanal.config;

import no.nav.altinnkanal.avro.NotifyTopicUpdate;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

@Configuration
public class KafkaConfiguration {
    @Value("${altinnkanal.topicMappingUpdate.topic}")
    private String topicMappingUpdateTopic;

    @Bean("kafkaProperties")
    public Properties kafkaProperties() throws Exception {
        Properties kafkaProperties = new Properties();
        kafkaProperties.load(getClass().getResourceAsStream("/kafka.properties"));
        return kafkaProperties;
    }

    @Bean("kafkaConsumerProperties")
    public Properties kafkaConsumerConfig(@Qualifier("kafkaProperties") Properties kafkaProperties) throws Exception {
        Properties consumerProperties = new Properties();
        consumerProperties.load(getClass().getResourceAsStream("/kafka_consumer.properties"));
        consumerProperties.putAll(kafkaProperties);
        consumerProperties.put("group.id", "altinnkanal-" + UUID.randomUUID());
        return consumerProperties;
    }

    @Bean
    public Producer<String, Object> producer(@Qualifier("kafkaProperties") Properties kafkaProperties) throws Exception {
        // Read kafka config
        return new KafkaProducer<>(kafkaProperties);
    }

    @Bean
    public Consumer<String, NotifyTopicUpdate> consumer(@Qualifier("kafkaConsumerProperties") Properties kafkaConsumerProperties) {
        KafkaConsumer<String, NotifyTopicUpdate> consumer = new KafkaConsumer<>(kafkaConsumerProperties);
        consumer.subscribe(Collections.singletonList(topicMappingUpdateTopic));
        return consumer;
    }
}
