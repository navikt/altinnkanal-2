package no.nav.altinnkanal.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.InputStreamReader

@Configuration
open class TopicConfiguration {
    private val objectMapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()

    @Bean
    open fun topicRouting(): TopicRouting =
        objectMapper.readValue(InputStreamReader(javaClass.getResourceAsStream(PATH)), TopicRouting::class.java)

    companion object {
        const val PATH = "/routing.yaml"
    }
}