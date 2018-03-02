package no.nav.altinnkanal.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.altinnkanal.JettyBootstrap
import java.io.InputStreamReader

const val PATH = "/routing.yaml"
private val objectMapper = ObjectMapper(YAMLFactory())
        .registerKotlinModule()

fun topicRouting(): TopicRouting =
        objectMapper.readValue(InputStreamReader(JettyBootstrap::class.java.getResourceAsStream(PATH)), TopicRouting::class.java)
