package no.nav.altinnkanal;

import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import kafka.server.KafkaServer;
import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.config.SoapProperties;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@ActiveProfiles("kafka-test")
@RunWith(SpringRunner.class)
@TestPropertySource({"classpath:application-test.properties"})
@SpringBootTest(
        classes = {BootstrapROBEA.class, OnlineBatchReceiverSoapIT.ITConfiguration.class},
        webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT
)

public class OnlineBatchReceiverSoapIT {
    @Autowired
    private SoapProperties soapProperties;

    @ClassRule
    public static KafkaEmbedded kafkaEmbedded = new KafkaEmbedded(1, true);

    @LocalServerPort
    private int localServerPort;
    private OnlineBatchReceiverSoap soapEndpoint;

    private String simpleBatch;
    private String simpleBatchMissingSec;

    private String createPayload(String serviceCode, String serviceEditionCode) {
        return simpleBatch
                .replaceAll("\\{\\{serviceCode}}", serviceCode)
                .replaceAll("\\{\\{serviceEditionCode}}", serviceEditionCode);
    }

    @Profile("kafka-test")
    @Configuration
    public static class ITConfiguration {
        @Autowired
        @Qualifier("schemaRegistryServer")
        private Server server;

        // Configure the bare minimal for running a schema registry and make sure the port is available
        @Bean
        public SchemaRegistryConfig schemaRegistryConfig() throws Exception {
            Properties properties = new Properties();
            properties.setProperty(SchemaRegistryConfig.LISTENERS_CONFIG, "http://0.0.0.0:" + SocketUtils.findAvailableTcpPort());
            properties.setProperty(SchemaRegistryConfig.KAFKASTORE_CONNECTION_URL_CONFIG, kafkaEmbedded.getZookeeperConnectionString());
            properties.setProperty(SchemaRegistryConfig.DEBUG_CONFIG, "true");

            return new SchemaRegistryConfig(properties);
        }

        // Configure the kafka client to use the spring embedded Kafka server and our schema registry
        @Bean("kafkaProperties")
        public Properties kafkaProperties(SchemaRegistryConfig schemaRegistryConfig) throws Exception {
            Properties kafkaProperties = new Properties();

            kafkaProperties.load(getClass().getResourceAsStream("/kafka.properties"));

            kafkaProperties.setProperty("bootstrap.servers", kafkaEmbedded.getBrokersAsString());
            kafkaProperties.setProperty("schema.registry.url", schemaRegistryConfig.getList(
                    SchemaRegistryConfig.LISTENERS_CONFIG).stream().collect(Collectors.joining(",")));
            kafkaProperties.setProperty("reconnect.backoff.max.ms", "15000");
            kafkaProperties.remove("security.protocol");

            return kafkaProperties;
        }

        // Embed a instance of the schema registry
        @Bean("schemaRegistryServer")
        public Server schemaRegistryServer(SchemaRegistryConfig schemaRegistryConfig) throws Exception {
            SchemaRegistryRestApplication schemaRegistryApplication = new SchemaRegistryRestApplication(schemaRegistryConfig);
            Server server = schemaRegistryApplication.createServer();
            server.start();
            return server;
        }

        // Clean up
        @PreDestroy
        public void stopSchemaRegistry() throws Exception {
            server.stop();
            server.join();
        }
    }

    private void stopKafkaServers()  {
        kafkaEmbedded.getKafkaServers().forEach(KafkaServer::shutdown);
        kafkaEmbedded.getKafkaServers().forEach(KafkaServer::awaitShutdown);
    }

    private void startKafkaServers() {
        kafkaEmbedded.getKafkaServers().forEach(KafkaServer::startup);
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

        // restart embedded Kafka
        startKafkaServers();
        result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, payload, new byte[0]);
        assertEquals("OK", result);
    }
}
