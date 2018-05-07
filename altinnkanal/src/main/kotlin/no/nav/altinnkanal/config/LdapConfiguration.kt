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

    fun init(config: Config) {
        this.config = config
    }
}