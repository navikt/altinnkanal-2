package no.nav.altinnkanal.soap

import org.apache.wss4j.common.ext.WSSecurityException
import org.apache.wss4j.dom.handler.RequestData
import org.apache.wss4j.dom.validate.Credential
import org.apache.wss4j.dom.validate.Validator
import org.slf4j.LoggerFactory
import java.util.Properties
import javax.naming.AuthenticationException
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext
import javax.naming.directory.SearchControls
import kotlin.reflect.jvm.jvmName

/**
 * Class for validating the WS-Security headers in incoming SOAP requests.
 * Basic flow:
 * 1. Attempt to find the username in AD under ServiceAccounts.
 * 2. Attempt to bind/login/authenticate to AD using the credentials from the UsernameToken.
 * Immediately throw a WSSecurityException if any of the checks above fail, otherwise return the credential (== valid).
 */
class LdapUntValidator : Validator {
    companion object {
        private val log = LoggerFactory.getLogger(LdapUntValidator::class.jvmName)
        private val ldapAdGroup = System.getenv("LDAP_AD_GROUP")
        private val ldapUrl = System.getenv("LDAP_URL")
        private val ldapUsername = System.getenv("LDAP_USERNAME")
        private val ldapPassword = System.getenv("LDAP_PASSWORD")
        private val ldapBaseDn = System.getenv("LDAP_SERVICEUSER_BASEDN")
        private val searchControls = SearchControls().apply {
            searchScope = SearchControls.SUBTREE_SCOPE
            returningAttributes = arrayOf("memberOf", "givenName")
            timeLimit = 30000
        }
        private val initProps = Properties().apply {
            put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
            put(Context.PROVIDER_URL, ldapUrl)
            put(Context.SECURITY_PRINCIPAL, ldapUsername)
            put(Context.SECURITY_CREDENTIALS, ldapPassword)
        }
    }

    override fun validate(credential: Credential, data: RequestData): Credential? {
        val username = credential.usernametoken.name
        val password = credential.usernametoken.password

        try {
            // Attempt to find the username in AD
            if (!findUsernameInAd(username)) throw WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION)

            // Attempt to bind the credentials for authentication
            val props = Properties().apply {
                put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
                put(Context.PROVIDER_URL, ldapUrl)
                put(Context.SECURITY_PRINCIPAL, username)
                put(Context.SECURITY_CREDENTIALS, password)
            }
            InitialDirContext(props).close()
            // Attempt to verify the group membership for the user
            if (!checkGroupMembershipInAd(username)) throw WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION)
        } catch (e: AuthenticationException) {
            log.error("User does not have valid credentials: ($username)")
            throw WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION)
        } catch (e: NamingException) {
            log.error("Connection to LDAP failed")
            throw RuntimeException("Could not initialize LDAP connection")
        }
        return credential
    }

    private fun findUsernameInAd(username: String) : Boolean {
        val namingEnum = InitialDirContext(initProps)
            .search(ldapBaseDn, "(cn=$username)", searchControls)
        // There should be exactly one match
        val result = if (!namingEnum.hasMoreElements()) {
            log.warn("User was not found in AD: ($username)")
            false
        } else {
            true
        }
        namingEnum.close()
        return result
    }

    private fun checkGroupMembershipInAd(username: String) : Boolean {
        val namingEnum = InitialDirContext(initProps)
                .search(System.getenv("LDAP_SERVICEUSER_BASEDN"), "(cn=$username)", searchControls)
        val groups = namingEnum.nextElement()
        val memberOf = groups.attributes.get("memberOf").all
        var result = false
        while (memberOf.hasMoreElements()) {
            val group = memberOf.nextElement().toString().substringAfter("=").substringBefore(",")
            if (group.equals(ldapAdGroup, true)) {
                log.debug("AD group membership found (user: $username, group: $group)")
                result = true
            }
        }
        memberOf.close()
        namingEnum.close()
        if (!result) log.warn("AD group membership not found (user: $username, group: $ldapAdGroup)")
        return result
    }
}
