package no.nav.altinnkanal.config

import no.nav.altinnkanal.avro.ExternalAttachment
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
open class KafkaConfiguration {
    @Bean("kafkaProperties")
    open fun kafkaProperties(): Properties {
        val kafkaProperties = Properties()
        kafkaProperties.load(javaClass.getResourceAsStream("/kafka.properties"))
        return kafkaProperties
    }

    @Bean
    open fun producer(@Qualifier("kafkaProperties") kafkaProperties: Properties): Producer<String, ExternalAttachment> {
        // Read kafka config
        return KafkaProducer(kafkaProperties)
    }
}