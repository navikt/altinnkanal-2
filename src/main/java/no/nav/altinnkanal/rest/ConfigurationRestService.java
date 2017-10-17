package no.nav.altinnkanal.rest;

import no.nav.altinnkanal.entities.TopicMappingUpdate;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.LogService;
import no.nav.altinnkanal.services.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.time.LocalDateTime;
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
    public List<TopicMappingUpdate> getTopicMappings() throws Exception {
        List<TopicMappingUpdate> listTopicMappings = logService.getUniqueChangelog(true);
        listTopicMappings.addAll(logService.getUniqueChangelog(false));
        return listTopicMappings;
    }

    @RequestMapping(method = POST)
    public TopicMappingUpdate createTopicMapping(@RequestBody TopicMappingUpdate update) throws Exception {
        String updatedUserId = "a_user";

        topicService.createTopicMapping(update.getServiceCode(), update.getServiceEditionCode(), update.getTopic(),
                update.isEnabled());

        logService.logChange(new TopicMappingUpdate(update.getServiceCode(), update.getServiceEditionCode(),
                update.getTopic(), update.isEnabled(), update.getComment(), LocalDateTime.now(), updatedUserId));

        return logService.getLastChangeFor(update.getServiceCode(), update.getServiceEditionCode());
    }

    @RequestMapping(method = PATCH, path = "/{serviceCode}/{serviceEditionCode}")
    public TopicMappingUpdate updateTopicMapping(@RequestBody TopicMappingUpdate updateRequest, @PathVariable String serviceCode, @PathVariable String serviceEditionCode) throws SQLException {
        TopicMappingUpdate old = logService.getLastChangeFor(serviceCode, serviceEditionCode);

        Boolean enabled = updateRequest.isEnabled() == null ? old.isEnabled() : updateRequest.isEnabled();
        String topic = updateRequest.getTopic() == null ? old.getTopic() : updateRequest.getTopic();
        String comment = updateRequest.getComment() == null ? old.getComment() : updateRequest.getComment();

        String user = "a_updating_user";

        logService.logChange(new TopicMappingUpdate(serviceCode, serviceEditionCode, topic, enabled, comment, LocalDateTime.now(), user));

        return logService.getLastChangeFor(updateRequest.getServiceCode(), updateRequest.getServiceEditionCode());
    }
}
