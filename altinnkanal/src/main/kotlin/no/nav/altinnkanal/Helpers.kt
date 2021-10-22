package no.nav.altinnkanal

import java.util.Base64

internal fun String.decodeBase64(): String = Base64.getDecoder().decode(this).toString(Charsets.UTF_8)
internal fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray())
inline fun <reified T : Enum<T>> ignoreCaseValueOf(name: String, defaultValue: T? = null): T =
    enumValues<T>().firstOrNull { it.name.equals(name, ignoreCase = true) }
        ?: defaultValue
        ?: throw IllegalArgumentException(
            "Invalid type '$name' for enum ${T::class.java.name}, valid types: ${enumValues<T>()
                .joinToString(prefix = "[", postfix = "]")}"
        )
