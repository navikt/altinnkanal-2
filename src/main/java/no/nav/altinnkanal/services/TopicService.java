package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;

import java.util.List;

public interface TopicService {
    TopicMapping getTopic(String serviceCode, String serviceEditionCode);

    TopicMapping createTopic(String serviceCode, String serviceEditionCode, String topic, String user, String comment);

    List<TopicMapping> getTopics();
}
