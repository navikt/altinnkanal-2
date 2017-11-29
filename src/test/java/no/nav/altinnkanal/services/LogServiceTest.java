package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMappingUpdate;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@RunWith(SpringRunner.class)
public class LogServiceTest {
    @Autowired
    private LogService logService;
    @Autowired
    private TopicService topicRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private Producer<String, Object> producer;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.execute("DELETE FROM `topic_mapping_log`;");
        jdbcTemplate.execute("DELETE FROM `topic_mappings`;");
    }

    private TopicMappingUpdate insertLog(String serviceCode, String serviceEditionCode, String topic, boolean enabled, String comment, LocalDateTime date, String updatedBy) throws Exception {
        return logService.logChange(new TopicMappingUpdate(serviceCode, serviceEditionCode, topic, enabled, comment, date, updatedBy));
    }

    @Test
    public void testLogChange() throws Exception {
        TopicMappingUpdate topicMappingUpdate = insertLog("test", "test","test.test", true, "This is a test",
                LocalDateTime.now(), "a_user");

        assertNotNull(topicMappingUpdate.getId());
    }

    @Test
    public void testResultMapping() throws Exception {
        final String serviceCode = "test";
        final String serviceEditionCode = "test2";
        final String topic = "test.test2";
        final boolean enabled = true;
        final String comment = "This is a test";
        final LocalDateTime date = LocalDateTime.now();
        final String updatedBy = "a_user";

        TopicMappingUpdate logInsert = insertLog(serviceCode, serviceEditionCode, topic, enabled, comment, date, updatedBy);
        topicRepository.createTopicMapping(serviceCode, serviceEditionCode, topic, logInsert.getId(), enabled);

        TopicMappingUpdate topicMappingUpdate = logService.getLastChangeFor(serviceCode, serviceEditionCode);

        assertNotNull(topicMappingUpdate.getId());
        assertEquals(serviceCode, topicMappingUpdate.getServiceCode());
        assertEquals(serviceEditionCode, topicMappingUpdate.getServiceEditionCode());
        assertEquals(topic, topicMappingUpdate.getTopic());
        assertEquals(enabled, topicMappingUpdate.isEnabled());
        assertEquals(comment, topicMappingUpdate.getComment());
        assertTrue("Time between local and persisted date is too big",
                date.until(topicMappingUpdate.getUpdateDate(), ChronoUnit.MINUTES) <= 5);
        assertEquals(updatedBy, topicMappingUpdate.getUpdatedBy());
    }

    @Test
    public void testGetLogs() throws Exception {
        TopicMappingUpdate last = null;
        for (int i = 0; i < 10; i++) {
            last = insertLog("test", "test","test.test", true,
                    "This is comment #" + i , LocalDateTime.now(), "a_user");
        }

        topicRepository.createTopicMapping("test", "test", "test.test", last.getId(), true);

        assertEquals(10, logService.getChangeLogFor("test", "test").size());
    }

    @Test
    public void getUniqueLogs() throws Exception {
        insertLog("test", "test", "test.test", true,
                "This is a comment", LocalDateTime.now(), "a_user");
        TopicMappingUpdate update1 = insertLog("test", "test", "test.test", false,
                "Had to disable this", LocalDateTime.now(), "another_user");
        TopicMappingUpdate update2 = insertLog("test", "test2", "test.test2", false,
                "I'm a comment!", LocalDateTime.now(), "a_user");
        TopicMappingUpdate update3 = insertLog("test", "test3", "test.test3", true,
                "Another one", LocalDateTime.now(), "a_user");


        topicRepository.createTopicMapping("test", "test", "test.test", update1.getId(), false);
        topicRepository.createTopicMapping("test", "test2", "test.test2", update2.getId(), false);
        topicRepository.createTopicMapping("test", "test3", "test.test3", update3.getId(), true);

        assertEquals(2, logService.getUniqueChangelog(false).size());
        assertEquals(1, logService.getUniqueChangelog(true).size());
    }
}
