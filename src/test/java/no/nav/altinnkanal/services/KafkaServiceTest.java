package no.nav.altinnkanal.services;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.NotifyTopicUpdate;
import no.nav.altinnkanal.entities.TopicMapping;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@RunWith(SpringRunner.class)
public class KafkaServiceTest {
    @MockBean
    private TopicService topicRepository;
    @MockBean
    private Producer<String, Object> producer;
    @MockBean
    private Consumer<String, NotifyTopicUpdate> consumer;

    @Autowired
    private OnlineBatchReceiverSoap onlineBatchReceiver;

    @Autowired
    private KafkaService kafkaService;

    @Mock
    private Future<RecordMetadata> future;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, Object>> argumentCaptor;

    private String simpleBatch;

    @Before
    public void setUp() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/data/basic_data_batch.xml")))) {
            simpleBatch = reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Test
    public void testPublishedToKafka() throws Exception {
        System.out.println(kafkaService);
        String expectedTopic = "test.test";

        when(topicRepository.getTopicMapping(anyString(), anyString())).thenReturn(new TopicMapping("test",
                "test", expectedTopic, 0, true));

        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.serializedValueSize()).thenReturn(0);

        when(future.get()).thenReturn(recordMetadata);

        when(producer.send(argumentCaptor.capture())).thenReturn(future);

        onlineBatchReceiver.receiveOnlineBatchExternalAttachment("", "", "", 0, simpleBatch, new byte[0]);

        ProducerRecord record = argumentCaptor.getValue();

        verify(producer, times(1)).send(any());
        assertEquals(expectedTopic, record.topic());
    }
}
