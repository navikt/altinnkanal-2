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
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.data_driven.data
import org.jetbrains.spek.data_driven.on
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

private const val TOPIC = "aapen-altinn-bankkontonummer-Mottatt"

object OnlineBatchReceiverSoapITSpec : Spek({
    val simpleBatch = "/data/basic_data_batch.xml".getResource()

    val kafkaEnvironment = KafkaEnvironment(
        noOfBrokers = 3,
        topics = listOf(TOPIC),
        withSchemaRegistry = true,
        withSecurity = true,
        users = listOf(JAASCredential(appConfig[KafkaConfig.username], appConfig[KafkaConfig.password])),
        autoStart = true
    )

    val adminClient: AdminClient? = kafkaEnvironment.adminClient
    adminClient.use {
        it!!.createAcls(createProducerAcl(TOPIC, appConfig[KafkaConfig.username])).all().get()
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
                .withBodyFile("sts-response-ok.json")
            )
        )
        stubFor(get(urlEqualTo(stsPath))
            .atPriority(2)
            .withBasicAuth("altinnkanal", "password")
            .willReturn(aResponse()
                .withStatus(HttpStatus.OK_200)
                .withHeader(HttpHeaders.ContentType, ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString())
                .withBodyFile("sts-response-ok-other-subject.json")
            )
        )
        stubFor(get(urlEqualTo(stsPath))
            .atPriority(3)
            .willReturn(aResponse()
                .withStatus(HttpStatus.UNAUTHORIZED_401)
                .withHeader(HttpHeaders.ContentType, ContentType.APPLICATION_JSON.withCharset(Charsets.UTF_8).toString())
                .withBodyFile("sts-response-invalid.json")
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

    context("SOAP requests") {
        given("invalid username tokens") {
            val payload = createPayload(simpleBatch, "2896", "87")
            on("usernametoken with %s",
                data("invalid password", "srvaltinnkanal", "wrongpassword", expected = SoapFault::class),
                data("non-accepted username", "altinnkanal", "password", expected = SoapFault::class),
                data("invalid username / ldap query injection", ") (&(cn=srvnotaltinnkanal)", "password",
                    expected = SoapFault::class)
            ) { _, username, password, expected ->
                it("should throw a SOAPFaultException") {
                    val client = createSoapClient(localServerPort, username, password)
                    val result =
                        { client.receiveOnlineBatchExternalAttachment(null, null, null,
                            0, payload, ByteArray(0))
                        }
                    result shouldThrow Exception::class withCause expected
                }
            }
        }

        given("valid usernametoken") {
            val soapClient = createSoapClient(localServerPort, validUsername, validPassword)
            on("a payload with %s",
                data("valid combination of SC and SEC",
                    createPayload(simpleBatch, "2896", "87"),
                    expected = Status.OK.name),
                data("non-routed combination of SC and SEC",
                    createPayload(simpleBatch, "1233", "1"),
                    expected = Status.FAILED_DO_NOT_RETRY.name),
                data("missing SC and/or SEC",
                    "/data/basic_data_batch_missing_sec.xml".getResource(),
                    expected = Status.FAILED.name)
            ) { _, payload, expected ->

                it("should return a result equal to $expected for batch") {
                    val result = soapClient.receiveOnlineBatchExternalAttachment(null, null, null,
                        0, payload, ByteArray(0)).getResultCode()

                    result shouldEqual expected
                }
            }

            on("a payload with valid combination of SC and SEC") {
                val payload = createPayload(simpleBatch, "2896", "87")

                on("%s",
                    data("Kafka temporarily down", ServerBase::stop, expected = Status.FAILED),
                    data("Kafka back up again", ServerBase::start, expected = Status.OK)
                ) { _, operation, expected ->
                    kafkaEnvironment.brokers.forEach(operation)

                    it("should return a result equal to $expected for batch") {
                        val result = soapClient.receiveOnlineBatchExternalAttachment(null, null, null,
                            0, payload, ByteArray(0)).getResultCode()

                        result shouldEqual expected
                    }
                }
            }
        }
    }

    context("self tests") {
        on("%s",
            data("is_alive", expected = APPLICATION_ALIVE),
            data("is_ready", expected = APPLICATION_READY)
        ) { path, expected ->
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
