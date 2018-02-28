package no.nav.altinnkanal.services;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.BootstrapROBEA;
import no.nav.altinnkanal.OnlineBatchReceiverSoapIT;
import no.nav.altinnkanal.Utils;
import no.nav.altinnkanal.avro.ExternalAttachment;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Ignore;
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

import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Ignore
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest(
        classes = {BootstrapROBEA.class, OnlineBatchReceiverSoapIT.ITConfiguration.class},
        webEnvironment =  SpringBootTest.WebEnvironment.RANDOM_PORT
)
@RunWith(SpringRunner.class)
public class KafkaServiceTest {
    @MockBean
    private TopicService topicRepository;
    @MockBean
    private Producer<String, Object> producer;

    @Autowired
    private OnlineBatchReceiverSoap onlineBatchReceiver;

    @Autowired
    private Producer<String, ExternalAttachment> kafkaProducer;

    @Mock
    private Future<RecordMetadata> future;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, Object>> argumentCaptor;

    private String simpleBatch;

    @Before
    public void setUp() throws Exception {
        simpleBatch = Utils.readToString("/data/basic_data_batch.xml");
    }

    @Test
    public void testPublishedToKafka() throws Exception {
        System.out.println(kafkaProducer);
        String expectedTopic = "test.test";

        when(topicRepository.getTopic(anyString(), anyString())).thenReturn(expectedTopic);

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
