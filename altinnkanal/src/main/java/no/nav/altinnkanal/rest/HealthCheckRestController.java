package no.nav.altinnkanal.rest;

import kotlin.text.Charsets;
import no.nav.altinnkanal.config.SoapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Controller
public class HealthCheckRestController {
    private final SoapProperties soapProperties;

    @Autowired
    public HealthCheckRestController(SoapProperties soapProperties) {
        this.soapProperties = soapProperties;
    }

    private List<Status> results = new ArrayList<>();

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final String APPLICATION_ALIVE = "Application is alive";
    private static final String APPLICATION_READY = "Application is ready";
    private static final String BASE_URL = "http://localhost:8080";
    private static final String WSDL_URL = BASE_URL + "/webservices/OnlineBatchReceiverSoap?wsdl";

    private final Logger logger = LoggerFactory.getLogger(HealthCheckRestController.class.getName());

    @ResponseBody
    @RequestMapping(value="isAlive", produces=MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isAlive() {
        return new ResponseEntity<>(APPLICATION_ALIVE, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="isReady", produces=MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isReady() {
        results.clear();
        results.add(httpUrlFetchTest(WSDL_URL));

        if (hasErrors(results)) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
    }

    private boolean hasErrors(List<Status> results) {
        return results.stream().anyMatch(Status.ERROR::equals);
    }

    private Status httpUrlFetchTest(String urlString) {
        HttpURLConnection httpConnection = null;
        try {
            String passwordString = soapProperties.getUsername() + ":" + soapProperties.getPassword();
            String encodedPassword = ENCODER.encodeToString(passwordString.getBytes(Charsets.UTF_8));
            httpConnection = (HttpURLConnection) new URL(urlString).openConnection();
            httpConnection.setRequestProperty("Authorization", "Basic " + encodedPassword);
            return httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK ? Status.OK : Status.ERROR;
        } catch (Exception e) {
            logger.error("HTTP endpoint readiness test failed", e);
            return Status.ERROR;
        } finally {
            if (httpConnection != null) httpConnection.disconnect();
        }
    }

    enum Status {
        OK,
        ERROR
    }
}
