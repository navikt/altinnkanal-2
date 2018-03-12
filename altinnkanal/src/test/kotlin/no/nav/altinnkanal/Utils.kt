package no.nav.altinnkanal

import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

object Utils {
    @Throws(IOException::class, URISyntaxException::class)
    fun readToString(resource: String): String {
        return String(Files.readAllBytes(Paths.get(Utils::class.java.getResource(resource).toURI())), Charset.forName("UTF-8"))
    }
}
