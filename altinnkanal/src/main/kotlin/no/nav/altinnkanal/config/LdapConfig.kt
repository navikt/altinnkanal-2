package no.nav.altinnkanal.config

object Ldap {
    data class Config(
        val adGroup: String = System.getenv("LDAP_AD_GROUP") ?: "0000-GA-altinnkanal-Operator",
        val url: String = System.getenv("LDAP_URL"),
        val username: String = System.getenv("LDAP_USERNAME"),
        val password: String = System.getenv("LDAP_PASSWORD"),
        val baseDn: String = System.getenv("LDAP_SERVICEUSER_BASEDN")
    )
    var override: Config? = null
    val config: Config by lazy { override ?: Config() }
}
