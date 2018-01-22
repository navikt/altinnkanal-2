package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;

import java.util.Collection;

public interface TopicRepository {
    TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception;

    Collection<TopicMapping> getTopicMappings() throws Exception;

    void createTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception;

    void updateTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception;
}
