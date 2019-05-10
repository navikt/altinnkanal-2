package no.nav.altinnkanal.services

import no.nav.altinnkanal.config.TopicRouting
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldNotBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TopicServiceSpec : Spek({
    val serviceCode = "testcode"
    val serviceEditionCode = "testeditioncode"
    val expectedTopics = listOf("test.testeditioncode")
    val topicService = TopicService(
        TopicRouting(listOf(TopicRouting.TopicRoute(serviceCode, serviceEditionCode, expectedTopics)))
    )

    describe("non-routed combinations of SC and SEC") {
        val invalidServiceCode = "missing"
        val invalidServiceEditionCode = "missing"
        context("getTopics") {
            val topic = topicService.getTopics(invalidServiceCode, invalidServiceEditionCode)
            it("should return null") {
                topic shouldBe null
            }
            it("should not return $expectedTopics") {
                topic shouldNotBe expectedTopics
            }
        }
    }

    describe("valid combinations of SC and SEC") {
        context("getTopics") {
            val topic = topicService.getTopics(serviceCode, serviceEditionCode)
            it("should not return null") {
                topic shouldNotBe null
            }
            it("should return $expectedTopics") {
                topic shouldBe expectedTopics
            }
        }
    }
})
