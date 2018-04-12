package no.nav.altinnkanal.soap

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import no.nav.altinnkanal.config.LdapConfiguration
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

/**
 * Class for validating UsernameTokens in incoming SOAP requests.
 * Basic flow:
 * 1. Attempt to find the username in AD under ServiceAccounts.
 * 2. Attempt to verify the group membership for the user
 * 3. Attempt to bind/login/authenticate to AD using the credentials from the UsernameToken.
 * Immediately throw a WSSecurityException if any of the checks above fail, otherwise return the credential (== valid).
 */
class LdapUntValidator: UsernameTokenValidator() {
    companion object {
        private val log = LoggerFactory.getLogger(LdapUntValidator::class.jvmName)
        private val searchControls = SearchControls().apply {
            searchScope = SearchControls.SUBTREE_SCOPE
            returningAttributes = arrayOf("memberOf", "givenName")
            timeLimit = 30000
        }
        private val boundedCache: Cache<String, Bounded> = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10)
            .build()
        private val config = LdapConfiguration.config
    }

    private data class Bounded(val username: String, val password: String)

    override fun validate(credential: Credential, data: RequestData): Credential {
        val username = credential.usernametoken.name
        val password = credential.usernametoken.password
        val initProps = Properties().apply {
            put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
            put(Context.PROVIDER_URL, config.url)
            put(Context.SECURITY_PRINCIPAL, config.username)
            put(Context.SECURITY_CREDENTIALS, config.password)
        }
        // Lookup provided user in cache to avoid unnecessary LDAP lookups
        boundedCache.getIfPresent(username)?.run {
            if (password == this.password) return credential
        }
        try {
            InitialDirContext(initProps).let {
                when {
                    !findUsernameInAd(username, it) ->
                        wsSecAuthFail("User was not found in AD: ($username)")
                    !checkGroupMembershipInAd(username, it) ->
                        wsSecAuthFail("AD group membership not found (user: $username, group: ${config.adGroup})")
                    else -> { }
                }
            }
            // Attempt to bind the credentials for authentication
            InitialDirContext(initProps.apply {
                put(Context.SECURITY_PRINCIPAL, "cn=$username,${config.baseDn}")
                put(Context.SECURITY_CREDENTIALS, password)
            }).close()
            // Cache the successful bind
            boundedCache.put(username, Bounded(username, password))
        } catch (e: AuthenticationException) {
            wsSecAuthFail("User does not have valid credentials: ($username)")
        } catch (e: NamingException) {
            log.error("Connection to LDAP failed")
            throw RuntimeException("Could not initialize LDAP connection")
        }
        return credential
    }

    private fun findUsernameInAd(username: String, initCtx: InitialDirContext): Boolean {
        // There should be exactly one match
        return initCtx.search(config.baseDn, "(cn=$username)", searchControls).run {
            when (hasMoreElements()){
                true -> {
                    nextElement()
                    !hasMoreElements()
                }
                else -> false
            }
        }
    }

    private fun checkGroupMembershipInAd(username: String, initCtx: InitialDirContext): Boolean {
        return initCtx
            .search(config.baseDn, "(cn=$username)", searchControls).nextElement()
            .attributes.get("memberOf").all.asSequence()
            .any { it.toString().substringAfter("=").substringBefore(",")
                .equals(config.adGroup, true)
            }
    }

    private fun wsSecAuthFail(message: String): Nothing {
        log.error(message)
        throw WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION)
    }
}
