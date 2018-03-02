package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.services.TopicService;
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class OnlineBatchReceiverSoapTest {
    @Mock
    private TopicService topicService;
    @Mock
    private Producer<String, ExternalAttachment> kafkaProducer;

    private OnlineBatchReceiverSoap soapService;

    private String simpleBatch;

    @Mock
    private Future<RecordMetadata> metadataFuture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        soapService = new OnlineBatchReceiverSoapImpl(topicService, kafkaProducer);
        simpleBatch = Utils.readToString("/data/basic_data_batch.xml");
    }

    @Test
    public void testFailedMissingTopic() throws Exception {
        when(topicService.getTopic(anyString(), anyString())).thenReturn(null);

        assertEquals("FAILED_DO_NOT_RETRY", soapService.receiveOnlineBatchExternalAttachment("username", "password", "123uhjoas", 0, simpleBatch, new byte[0]));
    }

    @Test
    public void testValidCall() throws Exception {
        when(topicService.getTopic(anyString(), anyString())).thenReturn("test");

        when(metadataFuture.get()).thenReturn(mock(RecordMetadata.class));

        when(kafkaProducer.send(any())).thenReturn(metadataFuture);

        assertEquals("OK", soapService.receiveOnlineBatchExternalAttachment("username", "password", "123uhjoas", 0, simpleBatch, new byte[0]));
    }
}
