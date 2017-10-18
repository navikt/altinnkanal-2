package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;

public interface TopicService {
    TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception;

    TopicMapping createTopicMapping(String serviceCode, String serviceEditionCode, String topic, Boolean enabled) throws Exception;

    TopicMapping updateTopicMapping(String serviceCode, String serviceEditionCode, String topic, Boolean enabled) throws Exception;
}
