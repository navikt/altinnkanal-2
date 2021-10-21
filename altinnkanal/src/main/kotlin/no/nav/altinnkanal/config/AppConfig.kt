package no.nav.altinnkanal.config

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SaslConfigs
import java.io.File
import java.util.Properties

private const val vaultApplicationPropertiesPath = "/var/run/secrets/nais.io/vault/application.properties"

val appConfig = if (System.getenv("APPLICATION_PROFILE") == "remote") {
    EnvironmentVariables() overriding
        systemProperties() overriding
        ConfigurationProperties.fromFile(File(vaultApplicationPropertiesPath)) overriding
        ConfigurationProperties.fromResource("local.properties")
} else {
    EnvironmentVariables() overriding
        systemProperties() overriding
        ConfigurationProperties.fromResource("local.properties")
}

object StsConfig {
    val stsValidUsername = appConfig[Key("sts.valid.username", stringType)]
    val stsUrl = appConfig[Key("sts.url", stringType)]
}

object KafkaConfig {
    val username = Key("srvaltinnkanal.username", stringType)
    val password = Key("srvaltinnkanal.password", stringType)
    val servers = Key("kafka.bootstrap.servers.url", stringType)

    val config = Properties().apply {
        load(KafkaConfig::class.java.getResourceAsStream("/kafka.properties"))
        setProperty(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${appConfig[KafkaConfig.username]}\" password=\"${appConfig[KafkaConfig.password]}\";")
        appConfig.getOrNull(KafkaConfig.servers)?.let {
            setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, it)
        }
        if (appConfig[Key("application.profile", stringType)] == "local")
            setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
    }
}
