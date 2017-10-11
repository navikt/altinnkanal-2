package no.nav.altinnkanal.rest;

import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.TopicService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;

@Path("/")
@Produces("application/json")
public class ConfigurationRestService {
    private final TopicService topicService;

    public ConfigurationRestService(TopicService topicService) {
        this.topicService = topicService;
    }

    @GET
    public List<TopicMapping> getTopicMappings() throws Exception {
        return topicService.getTopicMappings();
    }
}
