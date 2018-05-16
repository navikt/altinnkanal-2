package no.nav.altinnkanal.rest

import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

private const val APPLICATION_ALIVE = "Application is alive"
private const val APPLICATION_READY = "Application is ready"
private const val WSDL_URL = "http://localhost:8080/webservices/OnlineBatchReceiverSoap?wsdl"
private val logger = LoggerFactory.getLogger(SelfTest::class.java.name)

@Path("/")
class SelfTest {

    @Path("/isAlive")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getIsAlive(): String = APPLICATION_ALIVE

    @Path("/isReady")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    fun getIsReady(): Response =
        when (httpUrlFetchTest(WSDL_URL)) {
            Status.ERROR -> Response.serverError().build()
            Status.OK -> Response.ok(APPLICATION_READY).build()
        }

    private fun httpUrlFetchTest(urlString: String) =
        try {
            URL(urlString).openConnection().let {
                it as HttpURLConnection
                it.connect()
                if (it.responseCode == HttpURLConnection.HTTP_OK) Status.OK else Status.ERROR
            }
        } catch (e: Exception) {
            logger.error("HTTP endpoint readiness test failed", e)
            Status.ERROR
        }

    internal enum class Status {
        OK,
        ERROR
    }
}
