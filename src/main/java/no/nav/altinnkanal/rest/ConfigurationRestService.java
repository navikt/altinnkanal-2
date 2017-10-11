package no.nav.altinnkanal.rest;

import no.nav.altinnkanal.entities.TopicMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/configuration")
public class ConfigurationRestService {
    @RequestMapping(method = RequestMethod.GET)
    public List<TopicMapping> getTopicMappings() {
        return Collections.emptyList();
    }
}
