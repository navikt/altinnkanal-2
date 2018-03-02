package no.nav.altinnkanal.rest

import no.nav.altinnkanal.config.SoapProperties
import org.slf4j.LoggerFactory


import java.net.HttpURLConnection
import java.net.URL
import java.util.ArrayList
import java.util.Base64
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
class HealthCheckRestController constructor(private val soapProperties: SoapProperties) {

    private val results = ArrayList<Status>()

    private val logger = LoggerFactory.getLogger(HealthCheckRestController::class.java.name)

    @Path("/isAlive")
    @GET
    @Produces(javax.ws.rs.core.MediaType.TEXT_PLAIN)
    fun getIsAlive(): String = APPLICATION_ALIVE

    @Path("/isReady")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getIsReady(): Response {
        results.clear()
        results.add(httpUrlFetchTest(WSDL_URL))

        return if (hasErrors(results)) {
            Response.serverError().build()
        } else {
            Response.ok(APPLICATION_READY).build()
        }
    }

    private fun hasErrors(results: List<Status>): Boolean {
        return results.stream().anyMatch({ Status.ERROR == it })
    }

    private fun httpUrlFetchTest(urlString: String): Status {
        var httpConnection: HttpURLConnection? = null
        return try {
            val passwordString = soapProperties.username + ":" + soapProperties.password
            val encodedPassword = ENCODER.encodeToString(passwordString.toByteArray(Charsets.UTF_8))
            httpConnection = URL(urlString).openConnection() as HttpURLConnection
            httpConnection.setRequestProperty("Authorization", "Basic " + encodedPassword)
            if (httpConnection.responseCode == HttpURLConnection.HTTP_OK) Status.OK else Status.ERROR
        } catch (e: Exception) {
            logger.error("HTTP endpoint readiness test failed", e)
            Status.ERROR
        } finally {
            if (httpConnection != null) httpConnection.disconnect()
        }
    }

    internal enum class Status {
        OK,
        ERROR
    }

    companion object {

        private val ENCODER = Base64.getEncoder()
        private const val APPLICATION_ALIVE = "Application is alive"
        private const val APPLICATION_READY = "Application is ready"
        private const val BASE_URL = "http://localhost:8080"
        private const val WSDL_URL = BASE_URL + "/webservices/OnlineBatchReceiverSoap?wsdl"
    }
}