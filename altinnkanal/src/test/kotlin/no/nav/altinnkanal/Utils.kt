package no.nav.altinnkanal

import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.sdk.OperationType
import com.unboundid.ldap.sdk.schema.Schema
import com.unboundid.ldif.LDIFReader
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils
import no.altinn.webservices.OnlineBatchReceiverSoap
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.dom.WSConstants
import org.apache.wss4j.dom.handler.WSHandlerConstants
import java.io.InputStream
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import javax.security.auth.callback.CallbackHandler
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent

private object Utils
private val xmlInputFactory = XMLInputFactory.newFactory()

fun String.getResourceStream(): InputStream = Utils::class.java.getResourceAsStream(this)
fun String.getResource(): String = String(Files.readAllBytes(Paths.get(Utils::class.java.getResource(this).toURI())),
        Charset.forName("UTF-8"))
fun String.getResultCode(): String? =
    xmlInputFactory.createXMLStreamReader(StringReader(StringEscapeUtils.unescapeXml(this))).run {
        try {
            while (hasNext()) {
                if (next() == XMLEvent.START_ELEMENT && localName == "Result") {
                    return getAttributeValue(null, "resultCode").toString()
                }
            }
        } finally {
            close()
        }
        null
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
                Schema.getSchema("/ldap/memberOf.ldif".getResourceStream()),
                Schema.getSchema("/ldap/person.ldif".getResourceStream()))
        setAuthenticationRequiredOperationTypes(OperationType.COMPARE)
    }
    return InMemoryDirectoryServer(config)
        .apply {
            importFromLDIF(true, LDIFReader("/ldap/UsersAndGroups.ldif".getResourceStream()))
            startListening()
            mapOf(
                "ldap.ad.group" to adGroup,
                "ldap.url" to "ldap://127.0.0.1:$listenPort",
                "ldap.username" to "cn=$username,$baseDn",
                "ldap.password" to password,
                "ldap.serviceuser.basedn" to baseDn
            ).forEach { k, v -> System.setProperty(k, v) }
        }
}

fun createSoapClient(port: Int, username: String, password: String): OnlineBatchReceiverSoap {
    val props = mapOf(
        WSHandlerConstants.ACTION to WSHandlerConstants.USERNAME_TOKEN,
        WSHandlerConstants.USER to username,
        WSHandlerConstants.PASSWORD_TYPE to WSConstants.PW_TEXT,
        WSHandlerConstants.PW_CALLBACK_REF to CallbackHandler {
            (it[0] as WSPasswordCallback).password = password
        }
    )
    return JaxWsProxyFactoryBean().run {
        serviceClass = OnlineBatchReceiverSoap::class.java
        address = "http://localhost:$port/webservices/OnlineBatchReceiverSoap"
        outInterceptors.add(WSS4JOutInterceptor(props))
        create() as OnlineBatchReceiverSoap
    }
}
