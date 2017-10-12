package no.nav.altinnkanal.services;

import no.nav.altinnkanal.avro.ExternalAttachment;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Future;

@Service
public class KafkaServiceImpl implements KafkaService {
    private final Producer<String, byte[]> producer;

    @Autowired
    public KafkaServiceImpl(Producer<String, byte[]> producer) {
        this.producer = producer;
    }

    @Override
    public Future<RecordMetadata> publish(String topic, ExternalAttachment externalAttachment) throws IOException {
        return producer.send(new ProducerRecord<>(topic, encodeAvroObject(externalAttachment, ExternalAttachment.class)));
    }

    @Override
    public <T> byte[] encodeAvroObject(T avroObject, Class<T> cl) throws IOException {
        DatumWriter<T> datumWriter = new SpecificDatumWriter<>(cl);
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(byteOut, null);
        datumWriter.write(avroObject, encoder);
        encoder.flush();
        byteOut.close();
        return byteOut.toByteArray();
    }

    @PreDestroy
    public void destroy() {
        producer.close();
    }
}
