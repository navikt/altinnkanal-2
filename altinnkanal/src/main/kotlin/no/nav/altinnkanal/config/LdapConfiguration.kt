package no.nav.altinnkanal.config

object LdapConfiguration {

    data class Config(
        val adGroup: String,
        val url: String,
        val username: String,
        val password: String,
        val baseDn: String
    )

    lateinit var config: Config

    fun loadOverrideConfig(adGroup: String, url: String, username: String, password: String, baseDn: String) {
        config = Config(adGroup=adGroup, url=url, username=username, password=password, baseDn=baseDn)
    }

    fun loadDefaultConfig() {
        config = Config(adGroup = System.getenv("LDAP_AD_GROUP"),
            url = System.getenv("LDAP_URL"),
            username = System.getenv("LDAP_USERNAME"),
            password = System.getenv("LDAP_PASSWORD"),
            baseDn = System.getenv("LDAP_SERVICEUSER_BASEDN")
        )
    }

}