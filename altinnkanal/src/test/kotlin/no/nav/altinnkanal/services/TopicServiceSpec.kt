package no.nav.altinnkanal.services

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object TopicServiceSpec : Spek({
    val serviceCode = "testcode"
    val serviceEditionCode = "testeditioncode"
    val expectedTopic = "test.testeditioncode"
    val topicService = TopicService(
        TopicRouting(listOf(TopicRouting.TopicRoute(serviceCode, serviceEditionCode, expectedTopic)))
    )

    given("non-routed combinations of SC and SEC") {
        val invalidServiceCode = "missing"
        val invalidServiceEditionCode = "missing"
        on("getTopic") {
            val topic = topicService.getTopic(invalidServiceCode, invalidServiceEditionCode)
            it("should return null") {
                topic shouldBe null
            }
            it("should not return $expectedTopic") {
                topic shouldNotBe expectedTopic
            }
        }
    }

    given("valid combinations of SC and SEC") {
        on("getTopic") {
            val topic = topicService.getTopic(serviceCode, serviceEditionCode)
            it("should not return null") {
                topic shouldNotBe null
            }
            it ("should return $expectedTopic") {
                topic shouldBe expectedTopic
            }
        }
    }
})
