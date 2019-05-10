package no.nav.altinnkanal

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import io.ktor.http.HttpHeaders
import no.nav.altinnkanal.avro.ExternalAttachment
import no.nav.altinnkanal.config.KafkaConfig
import no.nav.altinnkanal.config.appConfig
import no.nav.altinnkanal.services.TopicService
import no.nav.altinnkanal.soap.Status
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import no.nav.common.JAASCredential
import no.nav.common.KafkaEnvironment
import no.nav.common.embeddedutils.ServerBase
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withCause
import org.apache.cxf.binding.soap.SoapFault
import org.apache.http.entity.ContentType
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.producer.KafkaProducer
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.Server
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

private val topics = listOf(
    "aapen-altinn-dokmot-Mottatt",
    "aapen-altinn-soning-Mottatt"
)

object OnlineBatchReceiverSoapITSpec : Spek({
    val simpleBatch = "/data/basic_data_batch.xml".getResource()

    val kafkaEnvironment = KafkaEnvironment(
        noOfBrokers = 3,
        topics = topics,
        withSchemaRegistry = true,
        withSecurity = true,
        users = listOf(JAASCredential(appConfig[KafkaConfig.username], appConfig[KafkaConfig.password])),
        autoStart = true
    )

    val adminClient: AdminClient? = kafkaEnvironment.adminClient
    adminClient.use {
        topics.forEach { topic ->
            it!!.createAcls(createProducerAcl(topic, appConfig[KafkaConfig.username])).all().get()
        }
    }

    val validUsername = "srvaltinnkanal"
    val validPassword = "supersecurepassword"
    System.setProperty("sts.valid.username", validUsername)

    val stsServer = WireMockServer(options().dynamicPort().notifier(Slf4jNotifier(true)))
        .also { it.start() }
    val stsPath = "/rest/v1/sts/token?grant_type=client_credentials&scope=openid"
    System.setProperty("sts.url", "http://localhost:${stsServer.port()}$stsPath")

    stsServer.apply {
        stubFor(get(urlEqualTo(stsPath))
            .atPriority(1)
            .withBasicAuth(validUsername, validPassword)
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK_200)
                .withHeader(HttpHeaders.ContentType, ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString())
                .withBody("""
                    {
                      "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzcnZhbHRpbm5rYW5hbCIsIm5hbWUiOiJKb2huIERvZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9.aNXjCqFzVlBvQscdNU8oyX2JgByAoXFWHWlNe_JJ8-bTJ16ls5bW1ICJ6CMAbC68-0jxlGtgQaZjC_pOtrkZPQvTkoEN0OKl3mPkR4PxgHAXP2KiH8HExFB2xjGetlOB_1-EuK0uMAuhRgeeFKCPz5AWNIcveTBY4nYlki3Ajuo",
                      "token_type": "Bearer",
                      "expires_in": 3600
                    }
                """.trimIndent())
            )
        )
        stubFor(get(urlEqualTo(stsPath))
            .atPriority(2)
            .withBasicAuth("altinnkanal", "password")
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK_200)
                .withHeader(HttpHeaders.ContentType, ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString())
                .withBody("""
                    {
                      "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbHRpbm5rYW5hbCIsIm5hbWUiOiJKb2huIERvZSIsImFkbWluIjp0cnVlLCJpYXQiOjE1MTYyMzkwMjJ9.gfAXuBuaLYUB9XulGazKOwFloWbRHaMzlgOq_uJkF9Nnyclcos_DBI8rIl2sZ56_vCJoaumxhzqBhHZcxhohzKO_fln-X2E8T8QLGX25d-c_K84XWPlKJjs8afESfOyRmLozEmIjgdTw9uk5e-hADhePQ6Qs3u_1NzoX-z0w9Is",
                      "token_type": "Bearer",
                      "expires_in": 3600
                    }
                """.trimIndent())
            )
        )
        stubFor(get(urlEqualTo(stsPath))
            .atPriority(3)
            .willReturn(aResponse()
                .withStatus(HttpStatus.UNAUTHORIZED_401)
                .withHeader(HttpHeaders.ContentType, ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString())
                .withBody("""
                    {
                      "error": "invalid_client"
                    }
                """.trimIndent())
            )
        )
    }

    val kafkaProperties = KafkaConfig.config.apply {
        setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaEnvironment.brokersURL)
        setProperty(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaEnvironment.schemaRegistry!!.url)
        setProperty(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, "1000")
    }
    val producer = KafkaProducer<String, ExternalAttachment>(kafkaProperties)
    val localServerPort = 47859
    val server = Server(localServerPort).apply {
        bootstrap(this, OnlineBatchReceiverSoapImpl(TopicService(), producer))
    }

    describe("SOAP requests") {
        context("invalid username tokens") {
            val payload = createPayload(simpleBatch, "2896", "87")
            listOf(
                Triple("invalid password", Pair("srvaltinnkanal", "wrongpassword"), SoapFault::class),
                Triple("non-accepted username", Pair("altinnkanal", "password"), SoapFault::class),
                Triple("invalid username / ldap query injection", Pair(") (&(cn=srvnotaltinnkanal)", "password"), SoapFault::class)
            ).forEach { (description, credentials, expected) ->
                context("usernametoken with $description") {
                    it("should throw a SOAPFaultException") {
                        val client = createSoapClient(localServerPort, credentials.first, credentials.second)
                        val result =
                            { client.receiveOnlineBatchExternalAttachment(null, null, null,
                                0, payload, ByteArray(0))
                            }
                        result shouldThrow Exception::class withCause expected
                    }
                }
            }
        }

        context("valid usernametoken") {
            val soapClient = createSoapClient(localServerPort, validUsername, validPassword)
            listOf(
                Triple("valid combination of SC and SEC",
                    createPayload(simpleBatch, "5152", "1"), Status.OK.name),
                Triple("non-routed combination of SC and SEC",
                    createPayload(simpleBatch, "1233", "1"), Status.FAILED_DO_NOT_RETRY.name),
                Triple("missing SC and/or SEC",
                    "/data/basic_data_batch_missing_sec.xml".getResource(), Status.FAILED.name)
            ).forEach { (description, payload, expected) ->
                context("a payload with $description") {
                    it("should return a result equal to $expected for batch") {
                        val result = soapClient.receiveOnlineBatchExternalAttachment(null, null, null,
                            0, payload, ByteArray(0)).getResultCode()
                        result shouldEqual expected
                    }
                }
            }

            context("a payload with valid combination of SC and SEC") {
                val payload = createPayload(simpleBatch, "5152", "1")
                listOf(
                    Triple("Kafka temporarily down", ServerBase::stop, Status.FAILED.name),
                    Triple("Kafka back up again", ServerBase::start, Status.OK.name)
                ).forEach { (description, operation, expected) ->
                    context(description) {
                        it("should return a result equal to $expected for batch") {
                            kafkaEnvironment.brokers.forEach(operation)
                            val result = soapClient.receiveOnlineBatchExternalAttachment(null, null, null,
                                0, payload, ByteArray(0)).getResultCode()

                            result shouldEqual expected
                        }
                    }
                }
            }
        }
    }

    describe("self tests") {
        listOf(
            "is_alive" to APPLICATION_ALIVE,
            "is_ready" to APPLICATION_READY
        ).forEach { (path, expected) ->
            context(path) {
                val conn = URL("http://localhost:$localServerPort/internal/$path").let {
                    it.openConnection() as HttpURLConnection
                }

                it("should return HTTP 200 OK") {
                    conn.responseCode shouldEqual HttpURLConnection.HTTP_OK
                }

                it("should return $expected") {
                    val response = Scanner(
                        URL("http://localhost:$localServerPort/internal/$path").openStream(), "UTF-8")
                        .useDelimiter("\\n").next()
                    response shouldEqual expected
                }
            }
        }
    }

    afterGroup {
        adminClient?.close()
        kafkaEnvironment.tearDown()
        producer.run {
            flush()
            close()
        }
        server.stop()
        stsServer.stop()
    }
})
