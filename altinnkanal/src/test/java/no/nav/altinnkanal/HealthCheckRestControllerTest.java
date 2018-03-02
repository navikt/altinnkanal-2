package no.nav.altinnkanal;

import no.nav.altinnkanal.config.SoapProperties;
import org.junit.Before;
import org.junit.Test;

public class HealthCheckRestControllerTest {

    private final SoapProperties soapCredentials = new SoapProperties("test", "test");
    private static final String APPLICATION_ALIVE = "Application is alive";

    @Before
    public void setUp() {
    }

    @Test
    public void testIsAlive() throws Exception {
    }

    @Test
    public void testIsReady() throws Exception {
    }
}
