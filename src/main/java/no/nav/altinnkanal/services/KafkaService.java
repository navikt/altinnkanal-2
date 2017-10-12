package no.nav.altinnkanal.services;

import no.nav.altinnkanal.avro.ExternalAttachment;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.IOException;
import java.util.concurrent.Future;

public interface KafkaService {
    Future<RecordMetadata> publish(String topic, ExternalAttachment externalAttachment) throws IOException;
    <T> byte[] encodeAvroObject(T avroObject, Class<T> cl) throws IOException;
}
