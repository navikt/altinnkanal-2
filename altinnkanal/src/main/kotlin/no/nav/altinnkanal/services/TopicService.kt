package no.nav.altinnkanal.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl
import java.io.InputStreamReader

const val PATH = "/routing.yaml"
private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
fun topicRouting(): TopicRouting =
        objectMapper.readValue(InputStreamReader(OnlineBatchReceiverSoapImpl::class.java.getResourceAsStream(PATH)), TopicRouting::class.java)

data class TopicRouting(val routes: List<TopicRoute>) {
    data class TopicRoute(val serviceCode: String, val serviceEditionCode: String, val topic: String)
}

open class TopicService constructor(config: TopicRouting) {
    private val routes: List<TopicRouting.TopicRoute> = config.routes

    open fun getTopic(serviceCode: String, serviceEditionCode: String): String? =
            routes.find { it.serviceCode == serviceCode && it.serviceEditionCode == serviceEditionCode }?.topic
}