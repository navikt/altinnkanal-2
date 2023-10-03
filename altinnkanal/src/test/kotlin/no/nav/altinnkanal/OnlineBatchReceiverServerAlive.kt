// package no.nav.altinnkanal
//
// import com.github.tomakehurst.wiremock.WireMockServer
// import com.github.tomakehurst.wiremock.client.WireMock.aResponse
// import com.github.tomakehurst.wiremock.client.WireMock.get
// import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
// import com.github.tomakehurst.wiremock.common.Slf4jNotifier
// import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
// import com.nhaarman.mockitokotlin2.mock
// import io.ktor.http.HttpHeaders
// import java.net.HttpURLConnection
// import java.net.URL
// import no.nav.altinnkanal.avro.ExternalAttachment
// import no.nav.altinnkanal.avro.ReceivedMessage
// import no.nav.altinnkanal.services.TopicService
// import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
// import org.amshove.kluent.shouldEqual
// import org.apache.http.entity.ContentType
// import org.apache.kafka.clients.producer.Producer
// import org.eclipse.jetty.http.HttpStatus
// import org.eclipse.jetty.server.Server
// import org.spekframework.spek2.Spek
// import org.spekframework.spek2.style.specification.describe
//
// object OnlineBatchReceiverServerAlive : Spek({
//
//    val validUsername = "srvaltinnkanal"
//    val validPassword = "supersecurepassword"
//    System.setProperty("sts.valid.username", validUsername)
//
//    val stsServer = WireMockServer(options().dynamicPort().notifier(Slf4jNotifier(true)))
//        .also { it.start() }
//    val stsPath = "/rest/v1/sts/token?grant_type=client_credentials&scope=openid"
//    System.setProperty("sts.url", "http://localhost:${stsServer.port()}$stsPath")
//
//    stsServer.apply {
//        stubFor(
//            get(urlEqualTo(stsPath))
//                .atPriority(1)
//                .withBasicAuth(validUsername, validPassword)
//                .willReturn(
//                    aResponse()
//                        .withStatus(HttpStatus.OK_200)
//                        .withHeader(
//                            HttpHeaders.ContentType,
//                            ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString()
//                        )
//                        .withBody(
//                            """
//                    {
//                      "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzcnZhbHRpbm5rYW5hbCIsIm5hbWUiOiJKb2huIERvZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9.aNXjCqFzVlBvQscdNU8oyX2JgByAoXFWHWlNe_JJ8-bTJ16ls5bW1ICJ6CMAbC68-0jxlGtgQaZjC_pOtrkZPQvTkoEN0OKl3mPkR4PxgHAXP2KiH8HExFB2xjGetlOB_1-EuK0uMAuhRgeeFKCPz5AWNIcveTBY4nYlki3Ajuo",
//                      "token_type": "Bearer",
//                      "expires_in": 3600
//                    }
//                            """.trimIndent()
//                        )
//                )
//        )
//        stubFor(
//            get(urlEqualTo(stsPath))
//                .atPriority(2)
//                .withBasicAuth("altinnkanal", "password")
//                .willReturn(
//                    aResponse()
//                        .withStatus(HttpStatus.OK_200)
//                        .withHeader(
//                            HttpHeaders.ContentType,
//                            ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString()
//                        )
//                        .withBody(
//                            """
//                    {
//                      "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbHRpbm5rYW5hbCIsIm5hbWUiOiJKb2huIERvZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9.gfAXuBuaLYUB9XulGazKOwFloWbRHaMzlgOq_uJkF9Nnyclcos_DBI8rIl2sZ56_vCJoaumxhzqBhHZcxhohzKO_fln-X2E8T8QLGX25d-c_K84XWPlKJjs8afESfOyRmLozEmIjgdTw9uk5e-hADhePQ6Qs3u_1NzoX-z0w9Is",
//                      "token_type": "Bearer",
//                      "expires_in": 3600
//                    }
//                            """.trimIndent()
//                        )
//                )
//        )
//        stubFor(
//            get(urlEqualTo(stsPath))
//                .atPriority(3)
//                .willReturn(
//                    aResponse()
//                        .withStatus(HttpStatus.UNAUTHORIZED_401)
//                        .withHeader(
//                            HttpHeaders.ContentType,
//                            ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString()
//                        )
//                        .withBody(
//                            """
//                            {
//                              "error": "invalid_client"
//                            }
//                            """.trimIndent()
//                        )
//                )
//        )
//    }
//
//    val localServerPort = 47850
//    var server: Server? = null
//
//    describe("self tests") {
//        beforeGroup {
//            val topicService = mock<TopicService>()
//            val kafkaProducer = mock<Producer<String, ExternalAttachment>>()
//            val avienProducer = mock<Producer<String, ReceivedMessage>>()
//            server = Server(localServerPort).apply {
//                bootstrap(
//                    this,
//                    OnlineBatchReceiverSoapImpl(
//                        topicService,
//                        kafkaProducer,
//                        avienProducer
//                    )
//                )
//            }
//        }
//
//        listOf(
//            "is_alive" to APPLICATION_ALIVE,
//            "is_ready" to APPLICATION_READY
//        ).forEach { (path, expected) ->
//            context(path) {
//                val conn = URL("http://localhost:$localServerPort/internal/$path").let {
//                    it.openConnection() as HttpURLConnection
//                }
//
//                it("should return HTTP 200 OK") {
//                    conn.responseCode shouldEqual HttpURLConnection.HTTP_OK
//                }
//
//                it("should return $expected") {
//                    val response = URL("http://localhost:$localServerPort/internal/$path").readText().trim()
//                    response shouldEqual expected
//                }
//            }
//        }
//        afterGroup {
//            server?.stop()
//            stsServer.stop()
//        }
//    }
// })
