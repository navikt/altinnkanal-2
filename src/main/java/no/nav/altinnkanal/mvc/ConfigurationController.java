package no.nav.altinnkanal.mvc;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.entities.TopicMappingUpdate;
import no.nav.altinnkanal.services.LogService;
import no.nav.altinnkanal.services.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static net.logstash.logback.marker.Markers.append;

@RequestMapping("/configuration")
@Controller
public class ConfigurationController {
    private final static String EDIT_ROLE_NAME = "ROLE_0000-GA-TEAM-INTEGRASJON";
    private final static String ROLE_CHECK = "hasRole('" + EDIT_ROLE_NAME + "')";
    private final LogService logService;
    private final TopicService topicService;
    private final Logger logger = LoggerFactory.getLogger(OnlineBatchReceiverSoap.class.getName());

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
    public ModelAndView listAllTopicMappings() throws Exception {
        List<TopicMappingUpdate> allMappings = new ArrayList<>(logService.getUniqueChangelog(true));
        allMappings.addAll(logService.getUniqueChangelog(false));
        return new ModelAndView("configuration")
                .addObject("topicMappingEntries", allMappings);
    }

    @GetMapping("/login")
    public ModelAndView getLogin(@RequestParam(name = "error", required = false) String error) throws Exception {
        return new ModelAndView("login")
                .addObject("error", error);
    }

    @PreAuthorize(ROLE_CHECK)
    @GetMapping("/new")
    public ModelAndView viewCreateTopicMapping() throws Exception {
        TopicMappingUpdate topic = new TopicMappingUpdate("", "", "", true, "", null, "");
        return new ModelAndView("edit_topic_mapping")
                .addObject("update", false)
                .addObject("topicMapping", topic);
    }

    @PreAuthorize(ROLE_CHECK)
    @PostMapping("/new")
    public ModelAndView createTopicMapping(Principal principal, @Valid CreateUpdateTopicMappingRequest update, BindingResult bindingResult) throws Exception {
        if (topicService.getTopicMapping(update.getServiceCode(), update.getServiceEditionCode()) != null) {
            return viewCreateTopicMappingError(update, "Error: Service Code and Service Edition Code combination already exists.");
        } else if (bindingResult.hasErrors()) {
            return viewCreateTopicMappingError(update, "Error: Service Code and/or Service Edition Code cannot be empty.");
        } else {
            TopicMappingUpdate topic = logService.logChange(new TopicMappingUpdate(update.getServiceCode(),
                    update.getServiceEditionCode(), update.getTopic(), update.isEnabled(), update.getComment(),
                    LocalDateTime.now(), principal.getName()));
            topicService.createTopicMapping(update.getServiceCode(), update.getServiceEditionCode(), update.getTopic(),
                    topic.getId(), update.isEnabled());
            logger.info(append("object", topic), "TopicMapping - New Entry");
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

    @PreAuthorize(ROLE_CHECK)
    @GetMapping("/{serviceCode}/{serviceEditionCode}/edit")
    public ModelAndView showEditTopicMapping(@PathVariable String serviceCode, @PathVariable String serviceEditionCode) throws Exception {
        TopicMappingUpdate topicMapping = logService.getLastChangeFor(serviceCode, serviceEditionCode);
        return new ModelAndView("edit_topic_mapping")
                .addObject("update", true)
                .addObject("topicMapping", topicMapping);
    }

    @PreAuthorize(ROLE_CHECK)
    @PostMapping("/{serviceCode}/{serviceEditionCode}/edit")
    public ModelAndView editTopicMapping(Principal principal, @PathVariable String serviceCode, @PathVariable String serviceEditionCode, CreateUpdateTopicMappingRequest update) throws Exception {
        TopicMappingUpdate topicMapping = logService.getLastChangeFor(serviceCode, serviceEditionCode);
        TopicMappingUpdate topicMappingUpdate = logService.logChange(new TopicMappingUpdate(update.getServiceCode(),
                update.getServiceEditionCode(), update.getTopic(), update.isEnabled(), update.getComment(),
                LocalDateTime.now(), principal.getName()));
        topicService.updateTopicMapping(serviceCode, serviceEditionCode, update.getTopic(), topicMappingUpdate.getId(), update.isEnabled());
        logger.info(append("old_object", topicMapping).and(append("new_object", topicMappingUpdate)), "TopicMapping - Changed Entry");
        return new ModelAndView("redirect:/configuration");
    }

    @PreAuthorize(ROLE_CHECK)
    @PostMapping("/{serviceCode}/{serviceEditionCode}/toggleEnabled")
    public ModelAndView toggleEnabledTopicMapping(Principal principal, @PathVariable String serviceCode, @PathVariable String serviceEditionCode) throws Exception {
        TopicMappingUpdate topicMapping = logService.getLastChangeFor(serviceCode, serviceEditionCode);
        TopicMappingUpdate topicMappingUpdate = logService.logChange(new TopicMappingUpdate(topicMapping.getServiceCode(),
                topicMapping.getServiceEditionCode(), topicMapping.getTopic(), !topicMapping.isEnabled(), topicMapping.getComment(),
                LocalDateTime.now(), principal.getName()));
        topicService.updateTopicMapping(serviceCode, serviceEditionCode, topicMapping.getTopic(), topicMappingUpdate.getId(), !topicMapping.isEnabled());
        logger.info(append("old_state", topicMapping.isEnabled()).and(append("new_state", !topicMapping.isEnabled())), "TopicMapping - Toggled Routing State");
        return new ModelAndView("redirect:/configuration");
    }
}
