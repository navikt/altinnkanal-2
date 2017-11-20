package no.nav.altinnkanal.services;

import no.nav.altinnkanal.avro.ExternalAttachment;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang.SerializationException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Future;

@Service
public class KafkaServiceImpl implements KafkaService {
    private final Producer<String, Object> producer;

    @Autowired
    public KafkaServiceImpl(Producer<String, Object> producer) {
        this.producer = producer;
    }

    @Override
    public Future<RecordMetadata> publish(String topic, ExternalAttachment externalAttachment) throws IOException, SerializationException {
        GenericRecord avroRecord = new GenericData.Record(externalAttachment.getSchema());
        avroRecord.put("batch", externalAttachment.getBatch());
        avroRecord.put("sc", externalAttachment.getSc());
        avroRecord.put("sec", externalAttachment.getSec());
        avroRecord.put("archRef", externalAttachment.getArchRef());
        return producer.send(new ProducerRecord<>(topic, avroRecord));
    }

    @PreDestroy
    public void destroy() {
        producer.close();
    }
}
