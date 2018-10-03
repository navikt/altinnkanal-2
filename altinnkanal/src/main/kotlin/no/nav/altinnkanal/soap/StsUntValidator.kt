package no.nav.altinnkanal.soap

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.util.decodeBase64
import io.ktor.util.encodeBase64
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import no.nav.altinnkanal.config.StsConfig
import org.apache.wss4j.common.ext.WSSecurityException
import org.apache.wss4j.dom.handler.RequestData
import org.apache.wss4j.dom.validate.Credential
import org.apache.wss4j.dom.validate.UsernameTokenValidator
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }
private val boundedCache: Cache<String, String> = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .maximumSize(10)
    .build()
private val httpClient: HttpClient = HttpClient(Apache)
private val objectMapper = jacksonObjectMapper().registerKotlinModule()

class StsUntValidator : UsernameTokenValidator() {

    override fun validate(credential: Credential, data: RequestData): Credential {
        val username = credential.usernametoken.name.let {
            if (it.matches(Regex("[\\w\\s]*"))) it
            else wsSecAuthFail("Invalid username: [$it]")
        }
        val password = credential.usernametoken.password
        val credentialsBase64Encoded = "$username:$password".encodeBase64()

        // Lookup provided user in cache to avoid unnecessary LDAP lookups
        boundedCache.getIfPresent(username)?.run { if (password == this) return credential }
        try {
            val jwt = runBlocking {
                val response = httpClient.get<HttpResponse> {
                    url(StsConfig.stsUrl)
                    header(HttpHeaders.Authorization, "Basic $credentialsBase64Encoded")
                }
                if (!response.status.isSuccess())
                    throw RuntimeException("Error response from STS: ${response.status.value} ${response.status.description}")

                response.readText()
            }

            val subject = objectMapper.readTree(jwt)
                .get("access_token")
                .asText()
                .substringAfter(".")
                .substringBefore(".")
                .decodeBase64()
                .let { objectMapper.readTree(it).get("sub").asText() }

            require(subject.equals(StsConfig.stsValidUsername, ignoreCase = true))
            boundedCache.put(username, password)
        } catch (e: Exception) {
            log.error(e) { }
            wsSecAuthFail("User does not have valid credentials: [$username]")
        }
        return credential
    }

    private fun wsSecAuthFail(message: String): Nothing {
        log.error { message }
        throw WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION)
    }
}
