package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class TopicServiceImpl implements TopicService {

    @Override
    public TopicMapping getTopic(String serviceCode, String serviceEditionCode) {
        // TODO: Resolve this from database
        return new TopicMapping(serviceCode, serviceEditionCode, "test", null, null, null);
    }

    @Override
    public TopicMapping createTopic(String serviceCode, String serviceEditionCode, String topic, String user, String comment) {
        return new TopicMapping(serviceCode, serviceEditionCode, topic, user, LocalDateTime.now(), comment);
    }

    @Override
    public List<TopicMapping> getTopics() {
        return Collections.emptyList();
    }
}
