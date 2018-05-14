package no.nav.altinnkanal.config

object LdapConfiguration {

    data class Config(
        val adGroup: String,
        val url: String,
        val username: String,
        val password: String,
        val baseDn: String
    )

    var override: Config? = null

    val config: Config by lazy {
    override ?: LdapConfiguration.Config(
            adGroup = System.getenv("LDAP_AD_GROUP") ?: "0000-GA-altinnkanal-Operator",
            url = System.getenv("LDAP_URL"),
            username = System.getenv("LDAP_USERNAME"),
            password = System.getenv("LDAP_PASSWORD"),
            baseDn = System.getenv("LDAP_SERVICEUSER_BASEDN")
        )
    }
}