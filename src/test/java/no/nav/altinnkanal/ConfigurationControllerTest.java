package no.nav.altinnkanal;

import no.nav.altinnkanal.entities.TopicMappingUpdate;
import no.nav.altinnkanal.mvc.ConfigurationController;
import no.nav.altinnkanal.mvc.CreateUpdateTopicMappingRequest;
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

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class ConfigurationControllerTest {

    private MockMvc mockMvc;
    @Mock private LogService logService;
    @Mock private TopicService topicService;
    @Mock private CreateUpdateTopicMappingRequest createUpdateTopicMappingRequest;
    @Mock private TopicMappingUpdate topicMappingUpdate;
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
    public void getConfigurationViewEnabledTest() throws Exception {
        mockMvc.perform(get("/configuration"))
                .andExpect(status().isOk())
                .andExpect(view().name("configuration"))
                .andExpect(model().attributeExists("topicMappingEntries", "enabled"))
                .andExpect(model().attribute("enabled", true));
    }

    @Test
    public void getConfigurationViewDisabledTest() throws Exception {
        mockMvc.perform(get("/configuration")
                .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("configuration"))
                .andExpect(model().attributeExists("topicMappingEntries", "enabled"))
                .andExpect(model().attribute("enabled", false));
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

    /*@Test
    public void postNewTopicMappingTest() throws Exception {
        // TODO: Make this work.
        long tmuId = 1;
        String cutmrTopic = "test";
        boolean cutmrEnabled = true;

        when(topicMappingUpdate.getId()).thenReturn(tmuId);
        when(createUpdateTopicMappingRequest.getTopic()).thenReturn(cutmrTopic);
        when(createUpdateTopicMappingRequest.isEnabled()).thenReturn(cutmrEnabled);

        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        TopicMappingUpdate tmu = new TopicMappingUpdate("test", "test", "test",
                true, "test", LocalDateTime.now(clock), "TEST_USER");
        when(logService.logChange(any(TopicMappingUpdate.class))).thenReturn(tmu);
        doNothing().when(topicService).updateTopicMapping(anyString(), anyString(), anyString(), anyLong(), anyBoolean()); // <- NPE - y tho?

        Principal principal = () -> "TEST_USER";
        mockMvc.perform(post("/configuration/{serviceCode}/{serviceEditionCode}/edit", "test", "test")
                .with(csrf())
                .principal(principal)
                .param("topicMapping.serviceCode", "test")
                .param("topicMapping.serviceEditionCode", "test")
                .param("topicMapping.topic", "test")
                .param("topicMapping.enabled", "true")
                .param("topicMapping.comment", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/configuration"))
                .andReturn();
    }

    @Test
    public void postEditTopicMappingTest() throws Exception {
        Principal principal = () -> "TEST_USER";
        mockMvc.perform(post("/configuration/{serviceCode}/{serviceEditionCode}/edit", "test", "test")
                .with(csrf())
                .principal(principal)
                .param("serviceCode", "test")
                .param("serviceEditionCode", "test")
                .param("topic", "test")
                .param("enabled", "true")
                .param("comment", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("redirect:/configuration"))
                .andReturn();
    }*/

}
