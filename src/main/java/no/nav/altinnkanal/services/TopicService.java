package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;

import java.util.List;

public interface TopicService {
    TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception;

    TopicMapping createTopicMapping(String serviceCode, String serviceEditionCode, String topic, Boolean enabled, String user, String comment) throws Exception;

    List<TopicMapping> getTopicMappings() throws Exception;
}
