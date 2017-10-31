package no.nav.altinnkanal.rest;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@PropertySource("classpath:kafka.properties")
@Controller
public class HealthCheckRestController {

    private static final String APPLICATION_ALIVE = "Application is alive";
    private static final String APPLICATION_READY = "Application is ready";
    private static final String BASE_URL = "http://localhost:8080/";
    private static final String WSDL_URL = BASE_URL + "altinnkanal/OnlineBatchReceiverSoap?wsdl";
    private static final String CONFIGURATION_URL = BASE_URL + "configuration";

    @Value("${bootstrap.servers}")
    private String KAFKA_BOOTSTRAP_SERVERS;
    private List<Boolean> checks;

    @ResponseBody
    @RequestMapping(value="isAlive", produces=MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isAlive() {
        return new ResponseEntity<>(APPLICATION_ALIVE, HttpStatus.OK);
    }

    @ResponseBody
    @RequestMapping(value="isReady", produces=MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity isReady() {
        System.out.println(KAFKA_BOOTSTRAP_SERVERS);
        boolean ready = selfTest();
        if (!ready) return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
    }

    private boolean selfTest() {
        checks = new ArrayList<>();
        checks.add(httpUrlFetchTest(WSDL_URL));
        checks.add(httpUrlFetchTest(CONFIGURATION_URL));
        checks.add(kafkaBrokerConnectionTest());
        for (boolean check : checks) if (!check) return false;
        return true;
    }

    private boolean httpUrlFetchTest(String urlString) {
        HttpURLConnection httpConnection = null;
        try {
            httpConnection = (HttpURLConnection) new URL(urlString).openConnection();
            return httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException ioe) {
            // TODO: Log the exception
            return false;
        } finally {
            if (httpConnection != null) httpConnection.disconnect();
        }
    }

    private boolean kafkaBrokerConnectionTest() {
        Properties props = new Properties();
        props.put("bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS);
        props.put("group.id", "test-group");
        props.put("enable.auto.commit", "true");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("request.timeout.ms", "5000");
        props.put("session.timeout.ms", "4000");
        props.put("heartbeat.interval.ms", "2500");
        props.put("fetch.max.wait.ms", "2500");
        KafkaConsumer kafkaConsumer = new KafkaConsumer<String, String>(props);
        try {
            kafkaConsumer.partitionsFor("connect-statuses");
            return true;
        } catch (Exception e) {
            // TODO: Log
            return false;
        } finally {
            kafkaConsumer.close();
        }
    }
}
