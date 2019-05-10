package no.nav.altinnkanal

import java.util.Base64

internal fun String.decodeBase64(): String = Base64.getDecoder().decode(this).toString(Charsets.UTF_8)
internal fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray())
