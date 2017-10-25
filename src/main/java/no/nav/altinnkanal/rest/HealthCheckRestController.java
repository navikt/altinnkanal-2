package no.nav.altinnkanal.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HealthCheckRestController {

    private static final String APPLICATION_ALIVE = "Application is alive";
    private static final String APPLICATION_READY = "Application is ready";

    @ResponseBody
    @RequestMapping(value="isAlive", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isAlive() {
        return new ResponseEntity<>(APPLICATION_ALIVE, HttpStatus.OK);
    }

    // TODO: Add tests to check exposed endpoints (ping?)
    @ResponseBody
    @RequestMapping(value="isReady", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isReady() {
        boolean ready = false;
        if (!ready) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
    }

}
