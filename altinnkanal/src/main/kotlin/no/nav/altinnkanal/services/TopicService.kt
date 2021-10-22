package no.nav.altinnkanal.services

import no.nav.altinnkanal.config.TopicRouting
import no.nav.altinnkanal.config.loadAivenRouting
import no.nav.altinnkanal.config.loadTopicRouting
import no.nav.altinnkanal.config.loadTopicRoutingV2

open class TopicService constructor(config: TopicRouting = loadTopicRouting()) {
    private val routes: List<TopicRouting.TopicRoute> = config.routes

    open fun getTopics(serviceCode: String, serviceEditionCode: String): List<String>? =
        routes.find { it.serviceCode == serviceCode && it.serviceEditionCode == serviceEditionCode }?.topics
}

open class TopicService2 constructor(config: TopicRouting = loadTopicRoutingV2()) {
    private val routes: List<TopicRouting.TopicRoute> = config.routes

    open fun getTopics(serviceCode: String, serviceEditionCode: String): List<String>? =
        routes.find { it.serviceCode == serviceCode && it.serviceEditionCode == serviceEditionCode }?.topics
}

open class AivenTopiccService constructor(config: TopicRouting = loadAivenRouting()) {
    private val routes: List<TopicRouting.TopicRoute> = config.routes

    open fun getTopics(serviceCode: String, serviceEditionCode: String): List<String>? =
        routes.find { it.serviceCode == serviceCode && it.serviceEditionCode == serviceEditionCode }?.topics
}
