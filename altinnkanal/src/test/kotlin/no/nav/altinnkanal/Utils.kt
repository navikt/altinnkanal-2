package no.nav.altinnkanal

import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.sdk.OperationType
import com.unboundid.ldap.sdk.schema.Schema
import com.unboundid.ldif.LDIFReader
import no.altinn.webservices.OnlineBatchReceiverSoap
import no.nav.altinnkanal.config.LdapConfiguration
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import java.io.IOException
import java.net.URISyntaxException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashMap
import javax.security.auth.callback.CallbackHandler

object Utils {

    @Throws(IOException::class, URISyntaxException::class)
    fun readToString(resource: String): String {
        return String(Files.readAllBytes(Paths.get(Utils::class.java.getResource(resource).toURI())), Charset.forName("UTF-8"))
    }

    fun createPayload(simpleBatch: String, serviceCode: String, serviceEditionCode: String): String {
        return simpleBatch
                .replace("\\{\\{serviceCode}}".toRegex(), serviceCode)
                .replace("\\{\\{serviceEditionCode}}".toRegex(), serviceEditionCode)
    }

    fun createLdapServer(): InMemoryDirectoryServer {
        val username = "srvadmin"
        val password = "password"
        val baseDn = "ou=ServiceAccounts,dc=testing,dc=local"
        val adGroup = "Operator-Altinnkanal"

        val config = InMemoryDirectoryServerConfig("dc=testing,dc=local").apply {
            schema = Schema.mergeSchemas(Schema.getDefaultStandardSchema(),
                    Schema.getSchema(Utils::class.java.getResourceAsStream("/ldap/memberOf.ldif")),
                    Schema.getSchema(Utils::class.java.getResourceAsStream("/ldap/person.ldif")))
            setAuthenticationRequiredOperationTypes(OperationType.COMPARE)
        }
        return InMemoryDirectoryServer(config)
            .apply {
                importFromLDIF(true, LDIFReader(Utils::class.java.getResourceAsStream("/ldap/UsersAndGroups.ldif")))
                startListening()
                LdapConfiguration.override = LdapConfiguration.Config(adGroup = adGroup, url = "ldap://127.0.0.1:$listenPort",
                    username = "cn=$username,$baseDn", password = password, baseDn = baseDn)
            }
    }

    fun createSoapClient(port: Int, username: String, password: String): OnlineBatchReceiverSoap {
        val outInterceptorProps = HashMap<String, Any>().apply {
            put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN)
            put(WSHandlerConstants.USER, username)
            put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT)
            put(WSHandlerConstants.PW_CALLBACK_REF, CallbackHandler { callbacks ->
                val pc = callbacks[0] as WSPasswordCallback
                pc.password = password
            })
        }
        return JaxWsProxyFactoryBean().run {
            serviceClass = OnlineBatchReceiverSoap::class.java
            address = "http://localhost:$port/webservices/OnlineBatchReceiverSoap"
            outInterceptors.add(WSS4JOutInterceptor(outInterceptorProps))
            create() as OnlineBatchReceiverSoap
        }
    }
}
