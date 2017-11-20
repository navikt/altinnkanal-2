package no.nav.altinnkanal;

import no.nav.altinnkanal.rest.HealthCheckRestController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class HealthCheckRestControllerTest {

    private MockMvc mockMvc;
    @InjectMocks private HealthCheckRestController hcrController;
    private static final String APPLICATION_ALIVE = "Application is alive";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(hcrController)
                .build();
    }

    @Test
    public void testIsAlive() throws Exception {
        mockMvc.perform(get("/isAlive").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andExpect(content().string(APPLICATION_ALIVE));
    }

    @Test
    public void testIsReady() throws Exception {
        mockMvc.perform(get("/isReady").accept(MediaType.TEXT_PLAIN))
            .andExpect(status().is5xxServerError());
    }
}
