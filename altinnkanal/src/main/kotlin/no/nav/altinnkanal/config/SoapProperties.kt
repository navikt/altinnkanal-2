package no.nav.altinnkanal.config

const val SOAP_USERNAME = "SOAP_USERNAME"
const val SOAP_PASSWORD = "SOAP_PASSWORD"

fun getVal(name: String): String =
        System.getenv(name) ?: throw RuntimeException("Missing variable: $name")

data class SoapProperties(
        val username: String = getVal(SOAP_USERNAME),
        val password: String = getVal(SOAP_PASSWORD)
)
