package no.nav.altinnkanal.services;

import no.nav.altinnkanal.avro.NotifyTopicUpdate;
import no.nav.altinnkanal.entities.TopicMapping;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@RunWith(SpringRunner.class)
public class TopicMappingServiceTest {
    @Autowired
    private LogService logService;
    @Autowired
    private TopicService topicRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private Producer<String, Object> producer;
    @MockBean
    private Consumer<String, NotifyTopicUpdate> consumer;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM `topic_mapping_log`;");
        jdbcTemplate.execute("DELETE FROM `topic_mappings`;");
    }

    @Test
    public void testReturnsNullMissingSeSec() throws Exception {
        TopicMapping topicMapping = topicRepository.getTopicMapping("missing", "missing");
        assertNull(topicMapping);
    }

    @Test
    public void testCreateTopic() throws Exception {
        final String serviceCode = "test";
        final String serviceEditionCode = "test";
        final String topic = "test.test";
        final long logEntry = 0;
        final boolean enabled = false;

        topicRepository.createTopicMapping(serviceCode, serviceEditionCode, topic, logEntry, enabled);
    }

    @Test
    public void testReturnsValidTopicMapping() throws Exception {
        final String serviceCode = "testcode";
        final String serviceEditionCode = "testeditioncode";
        final String topic = "test.testeditioncode";
        topicRepository.createTopicMapping(serviceCode, serviceEditionCode, topic, 0, true);

        TopicMapping topicMapping = topicRepository.getTopicMapping(serviceCode, serviceEditionCode);

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

        topicRepository.createTopicMapping("test", "test", "test.test", 0, true);

        topicRepository.updateTopicMapping(serviceCode, serviceEditionCode, newTopic, newLogEntry, newEnabled);

        TopicMapping topicMapping = topicRepository.getTopicMapping(serviceCode, serviceEditionCode);

        assertEquals(newTopic, topicMapping.getTopic());
        assertEquals(newLogEntry, topicMapping.getLogEntry());
        assertEquals(newEnabled, topicMapping.isEnabled());
    }
}
