package no.nav.altinnkanal.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStreamReader

private const val AIVEN_PATH = "/aiven-routing.yaml"

private val objectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

data class TopicRouting(val routes: List<TopicRoute>) {
    data class TopicRoute(val serviceCode: String, val serviceEditionCode: String, val topics: List<String>, val metadata: Map<String, String> = emptyMap())
}

fun loadAivenRouting(path: String = AIVEN_PATH): TopicRouting = objectMapper
    .readValue(InputStreamReader(TopicRouting::class.java.getResourceAsStream(path)), TopicRouting::class.java)
