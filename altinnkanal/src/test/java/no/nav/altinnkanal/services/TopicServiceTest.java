package no.nav.altinnkanal.services;

import no.nav.altinnkanal.config.TopicRouting;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TopicServiceTest {
    private static final String SERVICE_CODE = "testcode";
    private static final String SERVICE_EDITION_CODE = "testeditioncode";
    private static final String EXPECTED_TOPIC = "test.testeditioncode";

    private TopicRouting topicConfig = new TopicRouting(Collections.singletonList(
            new TopicRouting.TopicRoute(SERVICE_CODE, SERVICE_EDITION_CODE, EXPECTED_TOPIC)
    ));
    private TopicService topicService = new TopicService(topicConfig);

    @Test
    public void testReturnsNullMissingSeSec() {
        String topic = topicService.getTopic("missing", "missing");
        assertNull(topic);
    }

    @Test
    public void testTopicMappingNotNull() {

        String topic = topicService.getTopic(SERVICE_CODE, SERVICE_EDITION_CODE);

        assertNotNull(topic);

        assertEquals(EXPECTED_TOPIC, topic);
    }
}
