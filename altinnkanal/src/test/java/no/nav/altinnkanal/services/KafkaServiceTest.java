package no.nav.altinnkanal.services;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.Utils;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaServiceTest {
    @Mock
    private TopicService topicRepository;

    private OnlineBatchReceiverSoap onlineBatchReceiver;

    @Mock
    private Producer<String, ExternalAttachment> kafkaProducer;

    @Mock
    private Future<RecordMetadata> future;

    @Captor
    private ArgumentCaptor<ProducerRecord<String, ExternalAttachment>> argumentCaptor;

    private String simpleBatch;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        simpleBatch = Utils.readToString("/data/basic_data_batch.xml");
        onlineBatchReceiver = new OnlineBatchReceiverSoapImpl(topicRepository, kafkaProducer);
    }

    @Test
    public void testPublishedToKafka() throws Exception {
        System.out.println(kafkaProducer);
        String expectedTopic = "test.test";

        when(topicRepository.getTopic(anyString(), anyString())).thenReturn(expectedTopic);

        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        when(recordMetadata.serializedValueSize()).thenReturn(0);

        when(future.get()).thenReturn(recordMetadata);

        when(kafkaProducer.send(argumentCaptor.capture())).thenReturn(future);

        onlineBatchReceiver.receiveOnlineBatchExternalAttachment("", "", "", 0, simpleBatch, new byte[0]);

        ProducerRecord record = argumentCaptor.getValue();

        verify(kafkaProducer, times(1)).send(any());
        assertEquals(expectedTopic, record.topic());
    }
}
