package no.nav.altinnkanal.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStreamReader

private const val PATH = "/routing.yaml"
private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

fun loadTopicRouting(path: String = PATH): TopicRouting = objectMapper
    .readValue(InputStreamReader(TopicRouting::class.java.getResourceAsStream(path)), TopicRouting::class.java)

data class TopicRouting(val routes: List<TopicRoute>) {
    data class TopicRoute(val serviceCode: String, val serviceEditionCode: String, val topics: List<String>)
}
