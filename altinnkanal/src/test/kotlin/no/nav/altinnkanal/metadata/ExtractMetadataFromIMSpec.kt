package no.nav.altinnkanal.metadata

import no.nav.altinnkanal.batch.DataBatchExtractor
import no.nav.altinnkanal.config.TopicRouting
import no.nav.altinnkanal.getResource
import no.nav.altinnkanal.services.AivenTopiccService
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldNotBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class ExtractMetadataFromIMSpec : Spek({
    val serviceCode = "4936"
    val serviceEditionCode = "1"
    val expectedTopics = listOf("test.testeditioncode")
    val metadataConfig = mapOf("ytelse" to "ytelse")
    val topicService = AivenTopiccService(
        TopicRouting(listOf(TopicRouting.TopicRoute(serviceCode, serviceEditionCode, expectedTopics, metadataConfig)))
    )

    val imDataBatch = "/data/data_batch_im.xml".getResource()
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
    describe("get databatch and copy ytelse to metadata") {
        context("getBatch and extract") {
            val rm = DataBatchExtractor().toReceivedMessage(imDataBatch, "123-123")
            val meta = topicService.getMetaData(serviceCode, serviceEditionCode)
            it("should contain metadata config") {
                meta.size shouldBe 1
            }
            val test = XmlMetaData().extractDataFromMessage(rm, meta)
            it("should contain metadata ytelse key") {
                test shouldEqual true
                rm.getMetadata()["ytelse"] shouldEqual "Sykepenger"
            }
        }
    }
})
