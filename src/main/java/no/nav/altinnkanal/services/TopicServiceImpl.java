package no.nav.altinnkanal.services;

import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.entities.TopicMappingKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;

@Service
public class TopicServiceImpl implements TopicService {
    private HashMap<TopicMappingKey, TopicMapping> topicMappings;

    private TopicRepository repository;

    @Autowired
    public TopicServiceImpl(TopicRepository repository) throws Exception {
        this.repository = repository;

        initializeCache();
    }

    private void initializeCache() throws Exception {
        topicMappings = new HashMap<>();

        repository.getTopicMappings()
                .forEach(mapping -> topicMappings.put(TopicMappingKey.fromMapping(mapping), mapping));
    }

    @Override
    public TopicMapping getTopicMapping(String serviceCode, String serviceEditionCode) throws Exception {
        return topicMappings.get(new TopicMappingKey(serviceCode, serviceEditionCode));
    }

    @Override
    public Collection<TopicMapping> getTopicMappings() throws Exception {
        return topicMappings.values();
    }

    @Override
    public void createTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception {
        repository.createTopicMapping(serviceCode, serviceEditionCode, topic, logEntry, enabled);
        updateCache(serviceCode, serviceEditionCode);
    }

    @Override
    public void updateTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception {
        repository.updateTopicMapping(serviceCode, serviceEditionCode, topic, logEntry, enabled);
        updateCache(serviceCode, serviceEditionCode);
    }

    private void updateCache(String serviceCode, String serviceEditionCode) throws Exception {
        TopicMapping topicMapping = repository.getTopicMapping(serviceCode, serviceEditionCode);

        topicMappings.put(TopicMappingKey.fromMapping(topicMapping), topicMapping);
    }
}
