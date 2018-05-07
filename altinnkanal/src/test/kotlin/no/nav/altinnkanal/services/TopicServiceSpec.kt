package no.nav.altinnkanal.services

import no.nav.altinnkanal.config.TopicRouting
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

class TopicServiceSpec : Spek({
    val SERVICE_CODE = "testcode"
    val SERVICE_EDITION_CODE = "testeditioncode"
    val EXPECTED_TOPIC = "test.testeditioncode"
    val topicConfig = TopicRouting(listOf(TopicRouting.TopicRoute(SERVICE_CODE, SERVICE_EDITION_CODE, EXPECTED_TOPIC)))
    val topicService = TopicService(topicConfig)

    given("non-routed combinations of SC and SEC") {
        val serviceCode = "missing"
        val serviceEditionCode = "missing"
        on("getTopic") {
            val topic = topicService.getTopic(serviceCode, serviceEditionCode)
            it("should return null") {
                topic shouldBe null
            }
            it("should not return $EXPECTED_TOPIC") {
                topic shouldNotBe EXPECTED_TOPIC
            }
        }
    }

    given("valid combinations of SC and SEC") {
        on("getTopic") {
            val topic = topicService.getTopic(SERVICE_CODE, SERVICE_EDITION_CODE)
            it("should not return null") {
                topic shouldNotBe null
            }
            it ("should return $EXPECTED_TOPIC") {
                topic shouldBe EXPECTED_TOPIC
            }
        }
    }
})
