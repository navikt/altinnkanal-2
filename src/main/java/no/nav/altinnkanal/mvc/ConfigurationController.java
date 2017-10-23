package no.nav.altinnkanal.mvc;

import no.nav.altinnkanal.entities.TopicMappingUpdate;
import no.nav.altinnkanal.services.LogService;
import no.nav.altinnkanal.services.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

@RequestMapping("/configuration")
@Controller
public class ConfigurationController {
    private final LogService logService;
    private final TopicService topicService;
    @Autowired
    public ConfigurationController(LogService logService, TopicService topicService) {
        this.logService = logService;
        this.topicService = topicService;
    }

    public ModelAndView listAllTopicMappings(Boolean enabled) throws Exception {
        return new ModelAndView("configuration")
                .addObject("topicMappingEntries", logService.getUniqueChangelog(enabled))
                .addObject("enabled", enabled);
    }

    @GetMapping
    public ModelAndView listEnabledTopicMappings() throws Exception {
        return listAllTopicMappings(true);
    }

    @GetMapping("disabled")
    public ModelAndView listDisabledTopicMappings() throws Exception {
        return listAllTopicMappings(false);
    }

    @GetMapping("/new")
    public ModelAndView viewCreateTopicMapping() throws Exception {
        TopicMappingUpdate topic = new TopicMappingUpdate("", "", "", true, "", null, "");
        return new ModelAndView("edit_topic_mapping")
                .addObject("update", false)
                .addObject("topicMapping", topic);
    }

    @PostMapping("/new")
    public ModelAndView createTopicMapping(CreateUpdateTopicMappingRequest update) throws Exception {
        // TODO: check if logged in and use user id
        if (topicService.getTopicMapping(update.getServiceCode(), update.getServiceEditionCode()) != null) {
            return new ModelAndView("edit_topic_mapping")
                    .addObject("topicMapping", update)
                    .addObject("errorAlreadyExists", true);
        } else {
            TopicMappingUpdate topic = logService.logChange(new TopicMappingUpdate(update.getServiceCode(), update.getServiceEditionCode(), update.getTopic(), update.isEnabled(), update.getComment(), LocalDateTime.now(), "a_user"));
            topicService.createTopicMapping(update.getServiceCode(), update.getServiceEditionCode(), update.getTopic(), topic.getId(), update.isEnabled());
            return new ModelAndView("redirect:/configuration");
        }
    }

    @GetMapping("/{serviceCode}/{serviceEditionCode}")
    public ModelAndView showTopicMappingLog(@PathVariable String serviceCode, @PathVariable String serviceEditionCode) throws Exception {
        return new ModelAndView("topic_mapping_log")
                .addObject("serviceCode", serviceCode)
                .addObject("serviceEditionCode", serviceEditionCode)
                .addObject("log", logService.getChangeLogFor(serviceCode, serviceEditionCode));
    }

    @GetMapping("/{serviceCode}/{serviceEditionCode}/edit")
    public ModelAndView showEditTopicMapping(@PathVariable String serviceCode, @PathVariable String serviceEditionCode) throws Exception {
        TopicMappingUpdate topicMapping = logService.getLastChangeFor(serviceCode, serviceEditionCode);
        return new ModelAndView("edit_topic_mapping")
                .addObject("update", true)
                .addObject("topicMapping", topicMapping);
    }

    @PostMapping("/{serviceCode}/{serviceEditionCode}/edit")
    public ModelAndView editTopicMapping(@PathVariable String serviceCode, @PathVariable String serviceEditionCode, CreateUpdateTopicMappingRequest update) throws Exception {
        // TODO: check if logged in
        // TODO: Use user id here
        TopicMappingUpdate topicMappingUpdate = logService.logChange(new TopicMappingUpdate(update.getServiceCode(),
                update.getServiceEditionCode(), update.getTopic(), update.isEnabled(), update.getComment(),
                LocalDateTime.now(), "a_user"));
        topicService.updateTopicMapping(serviceCode, serviceEditionCode, update.getTopic(), topicMappingUpdate.getId(), update.isEnabled());
        return new ModelAndView("redirect:/configuration");
    }
}
