package no.nav.altinnkanal.config

import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.util.Properties

val appConfig = EnvironmentVariables() overriding
    systemProperties() overriding
    ConfigurationProperties.fromResource("local.properties")

object LdapConfig {
    val adGroup = appConfig[Key("ldap.ad.group", stringType)]
    val url = appConfig[Key("ldap.url", stringType)]
    val username = appConfig[Key("ldap.username", stringType)]
    val password = appConfig[Key("ldap.password", stringType)]
    val baseDn = appConfig[Key("ldap.serviceuser.basedn", stringType)]
}

object KafkaConfig {
    private val username = Key("srvaltinnkanal.username", stringType)
    private val password = Key("srvaltinnkanal.password", stringType)
    private val servers = Key("kafka.bootstrap.servers.url", stringType)

    val config = Properties().apply {
        load(KafkaConfig::class.java.getResourceAsStream("/kafka.properties"))
        setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${appConfig[KafkaConfig.username]}\" password=\"${appConfig[KafkaConfig.password]}\";")
        appConfig.getOrNull(KafkaConfig.servers)?.let {
            setProperty("bootstrap.servers", it)
        }
    }
}