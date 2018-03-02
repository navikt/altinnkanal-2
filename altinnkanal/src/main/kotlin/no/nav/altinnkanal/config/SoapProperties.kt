package no.nav.altinnkanal.config

const val SOAP_USERNAME = "soap.username"
const val SOAP_PASSWORD = "soap.password"

fun getVal(name: String): String =
        System.getenv(toEnvVar(name)) ?: throw RuntimeException("Missing variable: $name/${toEnvVar(name)}")

fun toEnvVar(name: String): String =
        name.replace(".", "_").toUpperCase()

data class SoapProperties(
        val username: String = getVal(SOAP_USERNAME),
        val password: String = getVal(SOAP_PASSWORD)
)