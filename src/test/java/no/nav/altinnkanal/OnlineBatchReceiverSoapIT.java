package no.nav.altinnkanal;

import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig;
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryRestApplication;
import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.entities.TopicMappingUpdate;
import no.nav.altinnkanal.services.LogService;
import no.nav.altinnkanal.services.TopicService;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.eclipse.jetty.server.Server;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@TestPropertySource({"classpath:application-test.properties"})
@SpringBootTest(
        classes = {BootstrapROBEA.class, OnlineBatchReceiverSoapIT.ITConfiguration.class},
        webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EmbeddedKafka
public class OnlineBatchReceiverSoapIT {
    @Autowired
    private LogService logService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Configuration
    public static class ITConfiguration {
        @Autowired
        private KafkaEmbedded kafkaEmbedded;
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

    private String readResource(String resourceFileName) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourceFileName)))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }


    @Before
    public void setUp() throws Exception {
        simpleBatch = readResource("/data/basic_data_batch.xml");
        simpleBatchMissingSec = readResource("/data/basic_data_batch_missing_sec.xml");

        jdbcTemplate.execute("DELETE FROM `topic_mapping_log`;");
        jdbcTemplate.execute("DELETE FROM `topic_mappings`;");

        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(OnlineBatchReceiverSoap.class);
        factory.setAddress("http://localhost:" + localServerPort + "/altinnkanal/OnlineBatchReceiverSoap");

        soapEndpoint = (OnlineBatchReceiverSoap) factory.create();
    }

    private void createMappingRoute(String serviceCode, String serviceEditionCode, String topic, boolean enabled) throws Exception {
        TopicMappingUpdate topicMappingUpdate = logService.logChange(new TopicMappingUpdate(serviceCode,
                serviceEditionCode, topic, enabled, "Test topic", LocalDateTime.now(), "t99999"));
        topicService.createTopicMapping(serviceCode, serviceEditionCode, topic, topicMappingUpdate.getId(), enabled);
    }


    @Test
    public void testValidScSec() throws Exception {
        final String serviceCode = "1232";
        final String serviceEditionCode = "4";
        final String topic = "test_topic_1232_4";

        createMappingRoute(serviceCode, serviceEditionCode, topic, true);

        String payload = createPayload(serviceCode, serviceEditionCode);

        String result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, payload, new byte[0]);

        assertEquals("OK", result);
    }

    @Test
    public void testDisabledScSec() throws Exception {
        final String serviceCode = "1233";
        final String serviceEditionCode = "1";
        final String topic = "test_topic_1233_1";

        createMappingRoute(serviceCode, serviceEditionCode, topic, false);

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

        createMappingRoute(serviceCode, serviceEditionCode, topic, true);

        String result = soapEndpoint.receiveOnlineBatchExternalAttachment(null, null,
                null, 0, simpleBatchMissingSec, new byte[0]);

        assertEquals("FAILED", result);
    }
}
