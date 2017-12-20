package no.nav.altinnkanal.services;

import io.prometheus.client.Summary;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.entities.TopicMappingKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;

import static net.logstash.logback.marker.Markers.append;

@Service
public class TopicServiceImpl implements TopicService {
    private final Logger logger = LoggerFactory.getLogger(TopicService.class.getName());
    private static final Summary cacheUpdateTime = Summary.build()
            .name("cache_update_time_ms").help("Time to update cache in ms")
            .create();

    private HashMap<TopicMappingKey, TopicMapping> topicMappings;

    private TopicRepository repository;
    private LogService logService;

    private long lastLogEntry = -1;

    @Autowired
    public TopicServiceImpl(TopicRepository repository, LogService logService) throws Exception {
        this.repository = repository;
        this.logService = logService;

        executeCacheUpdate(true);
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
        executeCacheUpdate(true);
    }

    @Override
    public void updateTopicMapping(String serviceCode, String serviceEditionCode, String topic, long logEntry, Boolean enabled) throws Exception {
        repository.updateTopicMapping(serviceCode, serviceEditionCode, topic, logEntry, enabled);
        executeCacheUpdate(true);
    }

    @Override
    public void updateCache(String serviceCode, String serviceEditionCode) throws Exception {
        TopicMapping topicMapping = repository.getTopicMapping(serviceCode, serviceEditionCode);

        topicMappings.put(TopicMappingKey.fromMapping(topicMapping), topicMapping);
    }

    @Scheduled(fixedRate = 1000)
    public void executeCacheUpdateTask() throws Exception {
        executeCacheUpdate(false);
    }

    private void executeCacheUpdate(boolean force) throws Exception {
        Summary.Timer cacheUpdateTimer = cacheUpdateTime.startTimer();

        logger.debug(append("force_update", force),
                "Querying database for topic updates {}", force);

        long lastPersistedLogEntry = logService.getLastLogEntryId();

        if (lastLogEntry >= lastPersistedLogEntry && !force) {
            logger.debug("No updates detected, aborting cache update");
            return;
        }

        HashMap<TopicMappingKey, TopicMapping> updated = new HashMap<>();

        repository.getTopicMappings()
                .forEach(mapping -> updated.put(TopicMappingKey.fromMapping(mapping), mapping));

        if (this.topicMappings == null) {
            logger.info("Initialized topic mapping cache");
        } else {
            updated.forEach((key, value) -> {
                if (topicMappings.containsKey(key) && topicMappings.get(key).equals(value))
                    return;
                logger.info(append("service_code", value.getServiceCode())
                            .and(append("service_edition_code", value.getServiceEditionCode()))
                            .and(append("new_entry", value)),
                    "Updating topic mapping: SC={}, SEC={}",
                    value.getServiceCode(), value.getServiceEditionCode());
            });
        }

        this.topicMappings = updated;
        this.lastLogEntry = lastPersistedLogEntry;

        cacheUpdateTimer.close();
    }
}
