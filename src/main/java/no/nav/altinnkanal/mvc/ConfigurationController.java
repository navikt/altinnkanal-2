package no.nav.altinnkanal.mvc;

import no.nav.altinnkanal.services.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

@Controller("/configuration")
public class ConfigurationController {
    private final LogService logService;
    @Autowired
    public ConfigurationController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/")
    public ModelAndView listAllTopicMappings() throws Exception {
        return new ModelAndView("configuration")
                .addObject("topicMappingEntries", logService.getUniqueChangelog());
    }

    @GetMapping("/{serviceCode}/{serviceEditionCode}")
    public ModelAndView showTopicMappingLog(@PathVariable String serviceCode, @PathVariable String serviceEditionCode) throws Exception {
        return new ModelAndView("topic_mapping_log")
                .addObject("log", logService.getChangeLogFor(serviceCode, serviceEditionCode));
    }
}
