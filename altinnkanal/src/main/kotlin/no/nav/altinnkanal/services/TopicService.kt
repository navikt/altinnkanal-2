package no.nav.altinnkanal.services

import no.nav.altinnkanal.config.TopicRouting
import no.nav.altinnkanal.config.loadTopicRouting

open class TopicService constructor(config: TopicRouting = loadTopicRouting()) {
    private val routes: List<TopicRouting.TopicRoute> = config.routes

    open fun getTopic(serviceCode: String, serviceEditionCode: String): String? =
        routes.find { it.serviceCode == serviceCode && it.serviceEditionCode == serviceEditionCode }?.topic
}
