package no.nav.altinnkanal.services

import no.nav.altinnkanal.config.TopicRouting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TopicServiceTest {

    private val topicConfig = TopicRouting(listOf(TopicRouting.TopicRoute(SERVICE_CODE, SERVICE_EDITION_CODE,
            EXPECTED_TOPIC)))
    private val topicService = TopicService(topicConfig)

    @Test
    fun testReturnsNullMissingSeSec() {
        val topic = topicService.getTopic("missing", "missing")
        assertNull(topic)
    }

    @Test
    fun testTopicMappingNotNull() {

        val topic = topicService.getTopic(SERVICE_CODE, SERVICE_EDITION_CODE)

        assertNotNull(topic)

        assertEquals(EXPECTED_TOPIC, topic)
    }

    companion object {
        private val SERVICE_CODE = "testcode"
        private val SERVICE_EDITION_CODE = "testeditioncode"
        private val EXPECTED_TOPIC = "test.testeditioncode"
    }
}
