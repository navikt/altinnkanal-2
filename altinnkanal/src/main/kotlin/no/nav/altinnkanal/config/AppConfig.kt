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
    private val adGroup = Key("ldap.ad.group", stringType)
    private val url = Key("ldap.url", stringType)
    private val username = Key("ldap.username", stringType)
    private val password = Key("ldap.password", stringType)
    private val baseDn = Key("ldap.serviceuser.basedn", stringType)

    data class Config(
        val adGroup: String = appConfig[LdapConfig.adGroup],
        val url: String = appConfig[LdapConfig.url],
        val username: String = appConfig[LdapConfig.username],
        val password: String = appConfig[LdapConfig.password],
        val baseDn: String = appConfig[LdapConfig.baseDn]
    )

    val config = Config()
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