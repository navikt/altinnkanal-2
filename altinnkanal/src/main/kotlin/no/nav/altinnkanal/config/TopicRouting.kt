package no.nav.altinnkanal.config

data class TopicRouting(
    val routes: List<TopicRoute>) {

    data class TopicRoute(
            val serviceCode: String,
            val serviceEditionCode: String,
            val topic: String
    )
}