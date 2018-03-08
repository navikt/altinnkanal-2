package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.config.SoapProperties;
import no.nav.altinnkanal.config.TopicConfigurationKt;
import no.nav.altinnkanal.services.TopicService;
import no.nav.altinnkanal.services.TopicServiceTest;
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl;
import no.nav.common.KafkaEnvironment;
import no.nav.common.embeddedutils.ServerBase;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.eclipse.jetty.server.Server;
import org.junit.*;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class OnlineBatchReceiverSoapIT {
    private static SoapProperties soapProperties = new SoapProperties("test", "test");
    private static int localServerPort;
    private static OnlineBatchReceiverSoap soapEndpoint;

    private String simpleBatch;
    private String simpleBatchMissingSec;
    private static KafkaEnvironment kafkaEnvironment;

    private String createPayload(String serviceCode, String serviceEditionCode) {
        return simpleBatch
                .replaceAll("\\{\\{serviceCode}}", serviceCode)
                .replaceAll("\\{\\{serviceEditionCode}}", serviceEditionCode);
    }

    public static int getRandomOpenPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(-1)) {
            return socket.getLocalPort();
        }
    }

    public static class ClientPasswordCallback implements CallbackHandler {
        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
            pc.setPassword(soapProperties.getPassword());
        }
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        kafkaEnvironment = new KafkaEnvironment(1, Collections.singletonList("aapen-altinn-bankkontonummerv87Mottatt-v1-preprod"), true, false, false);
        kafkaEnvironment.start();
        KafkaProducer<String, ExternalAttachment> producer = new KafkaProducer<>(kafkaProperties());
        TopicService topicService = new TopicService(TopicConfigurationKt.topicRouting());
        OnlineBatchReceiverSoapImpl batchReceiver = new OnlineBatchReceiverSoapImpl(topicService, producer);
        //localServerPort = getRandomOpenPort();
        localServerPort = 8123;
        Server server = new Server(localServerPort);
        JettyBootstrapKt.bootstrap(server, soapProperties, batchReceiver);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        kafkaEnvironment.tearDown();
    }

    private static Properties kafkaProperties() throws Exception {
        Properties kafkaProperties = new Properties();

        kafkaProperties.load(TopicServiceTest.class.getResourceAsStream("/kafka.properties"));

        kafkaProperties.setProperty("bootstrap.servers", kafkaEnvironment.getBrokersURL());
        kafkaProperties.setProperty("schema.registry.url", kafkaEnvironment.getServerPark().getSchemaregistry().getUrl());
        kafkaProperties.setProperty("request.timeout.ms", "1000");
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
        Map<String, Object> outProps = new HashMap<>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        outProps.put(WSHandlerConstants.USER, soapProperties.getUsername());
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        outProps.put(WSHandlerConstants.PW_CALLBACK_CLASS, ClientPasswordCallback.class.getName());
        WSS4JOutInterceptor outInterceptor = new WSS4JOutInterceptor(outProps);
        client.getOutInterceptors().add(outInterceptor);
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
        kafkaEnvironment.getServerPark().getBrokers().forEach(ServerBase::stop);
        String result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, payload, new byte[0]);
        assertEquals("FAILED", result);

        System.out.println("NEXT TEST");
        // restart embedded Kafka
        kafkaEnvironment.getServerPark().getBrokers().forEach(ServerBase::start);
        result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, payload, new byte[0]);
        assertEquals("OK", result);
    }
}
