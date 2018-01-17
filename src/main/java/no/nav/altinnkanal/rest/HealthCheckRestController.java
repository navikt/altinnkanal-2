package no.nav.altinnkanal.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@PropertySource("classpath:application.properties")
@Controller
public class HealthCheckRestController {

    @Value("${soap.username}")
    private String username;
    @Value("${soap.password}")
    private String password;

    private List<SelfTestResult> results = new ArrayList<>();

    private static final String APPLICATION_ALIVE = "Application is alive";
    private static final String APPLICATION_READY = "Application is ready";
    private static final String BASE_URL = "http://localhost:8080";
    private static final String WSDL_URL = BASE_URL + "/webservices/OnlineBatchReceiverSoap?wsdl";
    private static final String CONFIGURATION_URL = BASE_URL + "/configuration";

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
        results.add(httpUrlFetchTest(CONFIGURATION_URL));

        if (hasErrors(results)) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
    }

    private boolean hasErrors(List<SelfTestResult> results) {
        return results.stream().anyMatch(result -> result.getStatus() == Status.ERROR);
    }

    private SelfTestResult httpUrlFetchTest(String urlString) {
        HttpURLConnection httpConnection = null;
        try {
            httpConnection = (HttpURLConnection) new URL(urlString).openConnection();
            String encoded = base64EncodeToString(username + ":" + password);
            httpConnection.setRequestProperty("Authorization", "Basic "+encoded);
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                return new SelfTestResult(Status.OK);
            } else {
                return new SelfTestResult(Status.ERROR);
            }
        } catch (Exception e) {
            logger.error("HTTP endpoint readiness test failed", e);
            return new SelfTestResult(Status.ERROR);
        } finally {
            if (httpConnection != null) httpConnection.disconnect();
        }
    }

    private String base64EncodeToString(String stringToBeEncoded) {
        return Base64.getEncoder().encodeToString((stringToBeEncoded).getBytes(StandardCharsets.UTF_8));
    }

    enum Status {OK, ERROR}

    static class SelfTestResult {

        private Status status;

        SelfTestResult(Status status) {
            this.status = status;
        }

        Status getStatus() {
            return status;
        }
    }
}
