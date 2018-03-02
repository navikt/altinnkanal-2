package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.config.SoapProperties;
import no.nav.altinnkanal.config.TopicConfigurationKt;
import no.nav.altinnkanal.services.TopicService;
import no.nav.altinnkanal.services.TopicServiceTest;
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl;
import no.nav.common.KafkaEnvironment;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.eclipse.jetty.server.Server;
import org.junit.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class OnlineBatchReceiverSoapIT {
    private static SoapProperties soapProperties = new SoapProperties("test", "test");

    static Map<String, String> kafkaValues;

    private static int localServerPort;
    private static OnlineBatchReceiverSoap soapEndpoint;

    private String simpleBatch;
    private String simpleBatchMissingSec;

    private String createPayload(String serviceCode, String serviceEditionCode) {
        return simpleBatch
                .replaceAll("\\{\\{serviceCode}}", serviceCode)
                .replaceAll("\\{\\{serviceEditionCode}}", serviceEditionCode);
    }

    @BeforeClass
    public static void setupClass() throws Exception {

        setupKafka();
        KafkaProducer<String, ExternalAttachment> producer = new KafkaProducer<>(kafkaProperties());
        TopicService topicService = new TopicService(TopicConfigurationKt.topicRouting());
        OnlineBatchReceiverSoapImpl batchReceiver = new OnlineBatchReceiverSoapImpl(topicService, producer);
        Server server = new Server(8123);
        localServerPort = 8123;
        JettyBootstrapKt.bootstrap(server, soapProperties, batchReceiver);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        stopKafkaServers();
    }

    public static void setupKafka() throws Exception {
        kafkaValues = KafkaEnvironment.INSTANCE.start(1, Arrays.asList("aapen-altinn-bankkontonummerv87Mottatt-v1-preprod"));
    }

    private static void stopKafkaServers()  {
        KafkaEnvironment.INSTANCE.stop();
    }

    private static Properties kafkaProperties() throws Exception {
        Properties kafkaProperties = new Properties();

        kafkaProperties.load(TopicServiceTest.class.getResourceAsStream("/kafka.properties"));

        kafkaProperties.setProperty("bootstrap.servers", kafkaValues.get("broker"));
        kafkaProperties.setProperty("schema.registry.url", kafkaValues.get("schema"));
        kafkaProperties.setProperty("reconnect.backoff.max.ms", "15000");
        kafkaProperties.remove("security.protocol");

        return kafkaProperties;
    }

    @Before
    public void setUp() throws Exception {
        simpleBatch = Utils.readToString("/data/basic_data_batch.xml");
        simpleBatchMissingSec = Utils.readToString("/data/basic_data_batch_missing_sec.xml");

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(OnlineBatchReceiverSoap.class);
        factory.setAddress("http://localhost:" + localServerPort + "/webservices/OnlineBatchReceiverSoap");

        soapEndpoint = (OnlineBatchReceiverSoap) factory.create();
        Client client = ClientProxy.getClient(soapEndpoint);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        AuthorizationPolicy authPolicy = new AuthorizationPolicy();
        authPolicy.setUserName(soapProperties.getUsername());
        authPolicy.setPassword(soapProperties.getPassword());
        http.setAuthorization(authPolicy);
    }


    @Test
    public void testValidScSec() throws Exception {
        String payload = createPayload("2896", "87");

        String result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, payload, new byte[0]);

        assertEquals("OK", result);
    }

    @Test
    public void testNonRoutedScSec() throws Exception {
        final String serviceCode = "1233";
        final String serviceEditionCode = "1";
        final String topic = "test_topic_1233_1";

        String payload = createPayload(serviceCode, serviceEditionCode);

        String result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, payload, new byte[0]);

        assertEquals("FAILED_DO_NOT_RETRY", result);
    }

    @Test
    public void testMissingSecInPayload() throws Exception {
        final String serviceCode = "1233";
        final String serviceEditionCode = "2";
        final String topic = "test_topic_1233_2";

        String result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, simpleBatchMissingSec, new byte[0]);

        assertEquals("FAILED", result);
    }

    @Test
    public void testKafkaBrokerTemporarilyUnavailable() throws Exception {
        final String serviceCode = "2896";
        final String serviceEditionCode = "87";

        String payload = createPayload(serviceCode, serviceEditionCode);

        // shutdown embedded Kafka
        stopKafkaServers();
        String result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, payload, new byte[0]);
        assertEquals("FAILED", result);

        System.out.println("NEXT TEST");
        // restart embedded Kafka
        setupKafka();
        //result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
        //        null, 0, payload, new byte[0]);
        //assertEquals("OK", result);
    }
}
