package no.nav.altinnkanal.services

import no.nav.altinnkanal.config.TopicRouting
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
open class TopicService @Autowired constructor(config: TopicRouting) {
    private val routes: List<TopicRouting.TopicRoute> = config.routes

    open fun getTopic(serviceCode: String, serviceEditionCode: String): String? =
            routes.find { it.serviceCode == serviceCode && it.serviceEditionCode == serviceEditionCode }?.topic
}