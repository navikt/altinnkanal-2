package no.nav.altinnkanal

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File

private const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/application.properties"

private val config = if (System.getenv("APPLICATION_PROFILE") == "remote") {
    systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromFile(File(vaultApplicationPropertiesPath)) overriding
        ConfigurationProperties.fromResource("application.properties")
} else {
    systemProperties() overriding
        EnvironmentVariables() overriding
        ConfigurationProperties.fromResource("application.properties")
}

data class Environment(
    val application: Application = Application(),
    val stsConfig: StsConfig = StsConfig(),
    val kafkaProducer: KafkaProducer = KafkaProducer(),

) {
    data class Application(
        val profile: String = config.getOrElse(Key("application.profile", stringType), "local"),
        var username: String = config[Key("srvaltinnkanal.username", stringType)],
        var password: String = config[Key("srvaltinnkanal.password", stringType)]
    )

    data class StsConfig(
        val stsValidUsername: String = config[Key("sts.valid.username", stringType)],
        val stsUrl: String = config[Key("sts.url", stringType)]
    )

    data class KafkaProducer(
        val clientId: String = config[Key("client.id", stringType)],
        val acks: String = config[Key("acks", stringType)],
        val maxInFlightRequest: String = config[Key("max.in.flight.request", stringType)],
        val maxBlockMs: String = config[Key("max.block.ms", stringType)],
        val retries: String = config[Key("retries", stringType)],
        var bootstrapServers: String = config[Key("bootstrap.servers", stringType)],
        var username: String = config[Key("srvaltinnkanal.username", stringType)],
        var password: String = config[Key("srvaltinnkanal.password", stringType)],
        var schemaRegistryUrl: String = config[Key("schema.registry.url", stringType)],
        val saslMechanism: String = config[Key("sasl.mechanism", stringType)],
        val securityProtocol: String = config[Key("security.protocol", stringType)]
    )
}

enum class Profile {
    LOCAL_TEST, LOCAL, DEV, PREPROD, PRODUCTION
}
