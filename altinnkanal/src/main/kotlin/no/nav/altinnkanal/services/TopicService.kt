package no.nav.altinnkanal.services

import no.nav.altinnkanal.config.TopicRouting

open class TopicService constructor(config: TopicRouting) {
    private val routes: List<TopicRouting.TopicRoute> = config.routes

    open fun getTopic(serviceCode: String, serviceEditionCode: String): String? =
            routes.find { it.serviceCode == serviceCode && it.serviceEditionCode == serviceEditionCode }?.topic
}