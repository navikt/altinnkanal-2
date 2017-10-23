package no.nav.altinnkanal.mvc;

import no.nav.altinnkanal.entities.TopicMappingUpdate;
import no.nav.altinnkanal.services.LogService;
import no.nav.altinnkanal.services.TopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RequestMapping("/configuration")
@Controller
public class ConfigurationController {
    private final static String EDIT_ROLE_NAME = "";
    private final LogService logService;
    private final TopicService topicService;

    @Autowired
    public ConfigurationController(LogService logService, TopicService topicService) {
        this.logService = logService;
        this.topicService = topicService;
    }

    private ModelAndView viewCreateTopicMappingError(CreateUpdateTopicMappingRequest update, String errorMsg) throws Exception {
        return new ModelAndView("edit_topic_mapping")
                .addObject("topicMapping", update)
                .addObject("error", true)
                .addObject("errorMsg", errorMsg);
    }

    @GetMapping
    public ModelAndView listAllTopicMappings(@RequestParam(name = "enabled", defaultValue = "true") Boolean enabled) throws Exception {
        return new ModelAndView("configuration")
                .addObject("topicMappingEntries", logService.getUniqueChangelog(enabled))
                .addObject("enabled", enabled);
    }

    @GetMapping("/login")
    public ModelAndView getLogin(@RequestParam(name = "error", required = false) String error) throws Exception {
        return new ModelAndView("login")
                .addObject("error", error);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/new")
    public ModelAndView viewCreateTopicMapping() throws Exception {
        TopicMappingUpdate topic = new TopicMappingUpdate("", "", "", true, "", null, "");
        return new ModelAndView("edit_topic_mapping")
                .addObject("update", false)
                .addObject("topicMapping", topic);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/new")
    public ModelAndView createTopicMapping(@Valid CreateUpdateTopicMappingRequest update, BindingResult bindingResult) throws Exception {
        // TODO: check if logged in and use user id
        if (topicService.getTopicMapping(update.getServiceCode(), update.getServiceEditionCode()) != null) {
            return viewCreateTopicMappingError(update, "Error: Service Code and Service Edition Code combination already exists.");
        } else if (bindingResult.hasErrors()) {
            return viewCreateTopicMappingError(update, "Error: Service Code and/or Service Edition Code cannot be empty.");
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

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{serviceCode}/{serviceEditionCode}/edit")
    public ModelAndView showEditTopicMapping(@PathVariable String serviceCode, @PathVariable String serviceEditionCode) throws Exception {
        TopicMappingUpdate topicMapping = logService.getLastChangeFor(serviceCode, serviceEditionCode);
        return new ModelAndView("edit_topic_mapping")
                .addObject("update", true)
                .addObject("topicMapping", topicMapping);
    }

    @PreAuthorize("isAuthenticated()")
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
