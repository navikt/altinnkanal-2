package no.nav.altinnkanal.soap

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.apache.wss4j.common.ext.WSSecurityException
import org.apache.wss4j.dom.handler.RequestData
import org.apache.wss4j.dom.validate.Credential
import org.apache.wss4j.dom.validate.UsernameTokenValidator
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.naming.AuthenticationException
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls
import kotlin.reflect.jvm.jvmName

data class LdapConfig(
    val adGroup: String,
    val url: String,
    val username: String,
    val password: String,
    val baseDn: String
)
var overrideConfig: LdapConfig? = null
private val config: LdapConfig by lazy {
    overrideConfig ?: LdapConfig(
        adGroup = System.getenv("LDAP_AD_GROUP") ?: "0000-GA-altinnkanal-Operator",
        url = System.getenv("LDAP_URL"),
        username = System.getenv("LDAP_USERNAME"),
        password = System.getenv("LDAP_PASSWORD"),
        baseDn = System.getenv("LDAP_SERVICEUSER_BASEDN")
    )
}

private val log = LoggerFactory.getLogger(LdapUntValidator::class.jvmName)
private val searchControls = SearchControls().apply {
    searchScope = SearchControls.SUBTREE_SCOPE
    returningAttributes = arrayOf("memberOf", "givenName")
    timeLimit = 30000
}
private val boundedCache: Cache<String, String> = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .maximumSize(10)
    .build()

class LdapUntValidator : UsernameTokenValidator() {

    override fun validate(credential: Credential, data: RequestData): Credential {
        val username = validateInput(credential.usernametoken.name)
        val password = credential.usernametoken.password
        val initProps = Properties().apply {
            put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
            put(Context.PROVIDER_URL, config.url)
            put(Context.SECURITY_PRINCIPAL, config.username)
            put(Context.SECURITY_CREDENTIALS, config.password)
        }

        // Lookup provided user in cache to avoid unnecessary LDAP lookups
        boundedCache.getIfPresent(username)?.run {
            if (password == this) return credential
        }
        try {
            InitialDirContext(initProps).let {
                when {
                    !checkGroupMembershipInAd(username, it) -> {
                        it.close()
                        wsSecAuthFail("AD group membership not found [user: $username, group: ${config.adGroup}]")
                    }
                    else -> { it.close() }
                }
            }
            // Attempt to bind the credentials for authentication
            InitialDirContext(initProps.apply {
                put(Context.SECURITY_PRINCIPAL, "cn=$username,${config.baseDn}")
                put(Context.SECURITY_CREDENTIALS, password)
            }).close()
            // Cache the successful bind
            boundedCache.put(username, password)
        } catch (e: AuthenticationException) {
            wsSecAuthFail("User does not have valid credentials: [$username]")
        } catch (e: NamingException) {
            log.error("Connection to LDAP failed", e)
            throw RuntimeException("Could not initialize LDAP connection")
        }
        return credential
    }

    private fun checkGroupMembershipInAd(username: String, initCtx: InitialDirContext) = initCtx
        .search(config.baseDn, "(cn=$username)", searchControls).run {
            nextElement().attributes.get("memberOf").all.asSequence()
            .any { it.toString().substringAfter("=").substringBefore(",")
                    .equals(config.adGroup, true) }
            .also { close() }
        }

    private fun wsSecAuthFail(message: String): Nothing {
        log.error(message)
        throw WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION)
    }

    // Only allow usernames with alphanumeric characters and whitespaces
    private fun validateInput(input: String) =
        if (input.matches(Regex("[\\w\\s]*"))) input
        else wsSecAuthFail("Invalid username: [$input]")
}
