package no.nav.altinnkanal;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.entities.TopicMapping;
import no.nav.altinnkanal.services.KafkaService;
import no.nav.altinnkanal.services.TopicService;
import no.nav.altinnkanal.soap.OnlineBatchReceiverSoapImpl;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;


import java.io.InputStreamReader;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
public class OnlineBatchReceiverSoapTest {
    @Mock
    private TopicService topicService;
    @Mock
    private KafkaService kafkaService;

    private OnlineBatchReceiverSoap soapService;

    private String simpleBatch;

    @Mock
    private Future<RecordMetadata> metadataFuture;

    @Before
    public void setUp() throws Exception {
        soapService = new OnlineBatchReceiverSoapImpl(topicService, kafkaService);
        simpleBatch = IOUtils.toString(new InputStreamReader(getClass().getResourceAsStream("/data/basic_data_batch.xml")));
    }

    @Test
    public void testFailedMissingTopic() throws Exception {
        when(topicService.getTopicMapping(anyString(), anyString())).thenReturn(null);

        assertEquals("FAILED_DO_NOT_RETRY", soapService.receiveOnlineBatchExternalAttachment("username", "password", "123uhjoas", 0, simpleBatch, new byte[0]));
    }

    @Test
    public void testFailedDisabledTopic() throws Exception {
        TopicMapping topicMapping = new TopicMapping("test", "test", "test", 0, false);
        when(topicService.getTopicMapping(anyString(), anyString())).thenReturn(topicMapping);

        assertEquals("FAILED_DO_NOT_RETRY", soapService.receiveOnlineBatchExternalAttachment("username", "password", "123uhjoas", 0, simpleBatch, new byte[0]));
    }

    @Test
    public void testValidCall() throws Exception {
        TopicMapping topicMapping = new TopicMapping("test", "test", "test", 0, true);
        when(topicService.getTopicMapping(anyString(), anyString())).thenReturn(topicMapping);

        when(metadataFuture.get()).thenReturn(mock(RecordMetadata.class));

        when(kafkaService.publish(anyString(), any(ExternalAttachment.class))).thenReturn(metadataFuture);

        assertEquals("OK", soapService.receiveOnlineBatchExternalAttachment("username", "password", "123uhjoas", 0, simpleBatch, new byte[0]));
    }

    @Test
    public void testFailedToPublish() throws Exception {
        when(topicService.getTopicMapping(anyString(), anyString())).thenThrow(new Exception("Failed to get topic mapping"));

        assertEquals("FAILED", soapService.receiveOnlineBatchExternalAttachment("username", "password", "123uhjoas", 0, simpleBatch, new byte[0]));
    }
}
