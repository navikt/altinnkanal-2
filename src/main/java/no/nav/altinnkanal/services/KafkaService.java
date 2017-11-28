package no.nav.altinnkanal.services;

import no.nav.altinnkanal.avro.ExternalAttachment;
import no.nav.altinnkanal.avro.NotifyTopicUpdate;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public interface KafkaService {
    Future<RecordMetadata> publish(String topic, ExternalAttachment externalAttachment) throws IOException, ExecutionException, InterruptedException;
    Future<RecordMetadata> publish(String topic, NotifyTopicUpdate topicUpdate) throws IOException, ExecutionException, InterruptedException;
}
