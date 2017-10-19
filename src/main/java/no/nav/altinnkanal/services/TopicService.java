package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;

public interface TopicService {
    TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception;

    void createTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception;

    void updateTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception;
}
