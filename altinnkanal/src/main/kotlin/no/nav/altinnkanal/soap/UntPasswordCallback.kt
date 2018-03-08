package no.nav.altinnkanal.soap

import no.nav.altinnkanal.config.SoapProperties
import org.apache.wss4j.common.ext.WSPasswordCallback
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler

class UntPasswordCallback(private val soapProperties: SoapProperties): CallbackHandler {

    override fun handle(callbacks: Array<out Callback>?) {
        val pc = callbacks?.get(0) as WSPasswordCallback
        if (pc.identifier == soapProperties.username) {
            pc.password = soapProperties.password
        }
    }
}
