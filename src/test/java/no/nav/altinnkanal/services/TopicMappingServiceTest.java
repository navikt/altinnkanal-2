package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;
import org.apache.kafka.clients.producer.Producer;
import org.influxdb.InfluxDB;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@RunWith(SpringRunner.class)
public class TopicMappingServiceTest {
    @Autowired
    private LogService logService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private InfluxDB influxDB;
    @MockBean
    private InfluxService influxService;
    @MockBean
    private Producer<String, byte[]> producer;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM `topic_mapping_log`;");
        jdbcTemplate.execute("DELETE FROM `topic_mappings`;");
    }

    @Test
    public void testReturnsNullMissingSeSec() throws Exception {
        TopicMapping topicMapping = topicService.getTopicMapping("missing", "missing");
        assertNull(topicMapping);
    }

    @Test
    public void testCreateTopic() throws Exception {
        final String serviceCode = "test";
        final String serviceEditionCode = "test";
        final String topic = "test.test";
        final long logEntry = 0;
        final boolean enabled = false;

        topicService.createTopicMapping(serviceCode, serviceEditionCode, topic, logEntry, enabled);
    }

    @Test
    public void testReturnsValidTopicMapping() throws Exception {
        final String serviceCode = "testcode";
        final String serviceEditionCode = "testeditioncode";
        final String topic = "test.testeditioncode";
        topicService.createTopicMapping(serviceCode, serviceEditionCode, topic, 0, true);

        TopicMapping topicMapping = topicService.getTopicMapping(serviceCode, serviceEditionCode);

        assertNotNull(topicMapping);
        assertEquals(topic, topicMapping.getTopic());
    }

    @Test
    public void testUpdateServiceMapping() throws Exception {
        final String serviceCode = "test";
        final String serviceEditionCode = "test";
        final String newTopic = "newcode.newserviceeditioncode";
        final long newLogEntry = 123;
        final boolean newEnabled = false;

        topicService.createTopicMapping("test", "test", "test.test", 0, true);

        topicService.updateTopicMapping(serviceCode, serviceEditionCode, newTopic, newLogEntry, newEnabled);

        TopicMapping topicMapping = topicService.getTopicMapping(serviceCode, serviceEditionCode);

        assertEquals(newTopic, topicMapping.getTopic());
        assertEquals(newLogEntry, topicMapping.getLogEntry());
        assertEquals(newEnabled, topicMapping.isEnabled());
    }
}
