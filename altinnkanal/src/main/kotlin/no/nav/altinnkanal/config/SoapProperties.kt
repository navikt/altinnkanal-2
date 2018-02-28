package no.nav.altinnkanal.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("soap")
open class SoapProperties constructor(var username: String, var password: String) {
}