package no.nav.altinnkanal.rest;

import no.nav.altinnkanal.entities.LogEvent;
import no.nav.altinnkanal.entities.LogEventType;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.LogService;
import no.nav.altinnkanal.services.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@RestController
@RequestMapping("/configuration/api")
public class ConfigurationRestService {
    private final TopicService topicService;
    private final LogService logService;

    @Autowired
    public ConfigurationRestService(TopicService topicService, LogService logService) {
        this.topicService = topicService;
        this.logService = logService;
    }

    @RequestMapping(method = GET)
    public List<TopicMapping> getTopicMappings() throws Exception {
        return topicService.getTopicMappings();
        //return Collections.singletonList(new TopicMapping("test", "test", "test", true, "userId", LocalDateTime.now(), "Comment"));
    }

    @RequestMapping(method = POST)
    public TopicMapping createTopicMapping(@RequestBody TopicMapping topicMapping) throws Exception {
        String updatedUserId = "a_user";

        topicService.createTopicMapping(topicMapping.getServiceCode(), topicMapping.getServiceEditionCode(),
                topicMapping.getTopic(), topicMapping.isEnabled(), updatedUserId, topicMapping.getComment());

        logService.logChange(new LogEvent(topicMapping.getServiceCode(), topicMapping.getServiceEditionCode(),
                null, topicMapping.getTopic(), LogEventType.CREATE, LocalDateTime.now(), updatedUserId));

        return topicService.getTopicMapping(topicMapping.getServiceCode(), topicMapping.getServiceEditionCode());
    }
}
