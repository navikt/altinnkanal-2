package no.nav.altinnkanal;

import no.nav.altinnkanal.mvc.ConfigurationController;
import no.nav.altinnkanal.services.LogService;
import no.nav.altinnkanal.services.TopicService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
public class ConfigurationControllerTest {

    private MockMvc mockMvc;
    @Mock private LogService logService;
    @Mock private TopicService topicService;
    @InjectMocks private ConfigurationController controller;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/main/resources/templates/");
        viewResolver.setSuffix(".mustache");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void getConfigurationViewTest() throws Exception {
        mockMvc.perform(get("/configuration"))
                .andExpect(status().isOk())
                .andExpect(view().name("configuration"))
                .andExpect(model().attributeExists("topicMappingEntries"));
    }

    @Test
    public void getLoginViewTest() throws Exception {
        mockMvc.perform(get("/configuration/login"))
                .andExpect(status().isOk());
    }

    @Test
    public void getNewViewTest() throws Exception {
        mockMvc.perform(get("/configuration/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_topic_mapping"))
                .andExpect(model().attributeExists("update"))
                .andExpect(model().attribute("update", false));
    }

    @Test
    public void getScSecViewTest() throws Exception {
        mockMvc.perform(get("/configuration/{serviceCode}/{serviceEditionCode}", "test", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("topic_mapping_log"))
                .andExpect(model().attributeExists("serviceCode", "serviceEditionCode", "log"))
                .andExpect(model().attribute("serviceCode", "test"))
                .andExpect(model().attribute("serviceEditionCode", "test"));
    }

    @Test
    public void getScSecEditViewTest() throws Exception {
        mockMvc.perform(get("/configuration/{serviceCode}/{serviceEditionCode}/edit", "test", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_topic_mapping"))
                .andExpect(model().attributeExists("update"))
                .andExpect(model().attribute("update", true));
    }
}
