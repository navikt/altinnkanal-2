package no.nav.altinnkanal.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import no.nav.altinnkanal.Environment
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer

private const val JAVA_KEYSTORE = "jks"
private const val PKCS12 = "PKCS12"

private fun envOrThrow(envVar: String) =
    System.getenv()[envVar] ?: throw IllegalStateException("$envVar er påkrevd miljøvariabel")

fun onPremProducerConfig(kafkaProp: Environment.KafkaProducer) = mutableMapOf<String, Any>(
    CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafkaProp.bootstrapServers,
    ProducerConfig.ACKS_CONFIG to kafkaProp.acks,
    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
    ProducerConfig.MAX_BLOCK_MS_CONFIG to kafkaProp.maxBlockMs,
    ProducerConfig.RETRIES_CONFIG to kafkaProp.retries,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
    ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to kafkaProp.maxInFlightRequest,
    ProducerConfig.MAX_REQUEST_SIZE_CONFIG to 15728640,
    ProducerConfig.COMPRESSION_TYPE_CONFIG to "snappy",
    SaslConfigs.SASL_JAAS_CONFIG to "org.apache.kafka.common.security.plain.PlainLoginModule required " +
        "username=\"${kafkaProp.username}\" password=\"${kafkaProp.password}\";",
    AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaProp.schemaRegistryUrl,
    SaslConfigs.SASL_MECHANISM to kafkaProp.saslMechanism,
    CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to kafkaProp.securityProtocol,
    CommonClientConfigs.CLIENT_ID_CONFIG to kafkaProp.clientId
)

fun aivenProducerConfig(kafkaProp: Environment.KafkaProducer) = mutableMapOf<String, Any>(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to envOrThrow("KAFKA_BROKERS"),
    ProducerConfig.ACKS_CONFIG to kafkaProp.acks,
    ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
    ProducerConfig.MAX_BLOCK_MS_CONFIG to kafkaProp.maxBlockMs,
    ProducerConfig.RETRIES_CONFIG to kafkaProp.retries,
    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.canonicalName,
    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java.canonicalName,
    ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to kafkaProp.maxInFlightRequest,
    ProducerConfig.MAX_REQUEST_SIZE_CONFIG to 15728640,
    CommonClientConfigs.CLIENT_ID_CONFIG to kafkaProp.clientId
) +
    sslConfig() + schemaRegistryConfig()

private fun sslConfig(): Map<String, String> {
    return mapOf(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
        SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "", // Disable server host name verification
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to JAVA_KEYSTORE,
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to PKCS12,
        SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_TRUSTSTORE_PATH"),
        SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
        SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to envOrThrow("KAFKA_KEYSTORE_PATH"),
        SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD"),
        SslConfigs.SSL_KEY_PASSWORD_CONFIG to envOrThrow("KAFKA_CREDSTORE_PASSWORD")
    )
}

private fun schemaRegistryConfig(): Map<String, String> {
    val schemaRegUsername = envOrThrow("KAFKA_SCHEMA_REGISTRY_USER")
    val schemaRegPassword = envOrThrow("KAFKA_SCHEMA_REGISTRY_PASSWORD")
    return mapOf(
        AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
        AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG to "$schemaRegUsername:$schemaRegPassword",
        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to envOrThrow("KAFKA_SCHEMA_REGISTRY")
    )
}
