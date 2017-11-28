package no.nav.altinnkanal.tasks;

import no.altinn.webservices.OnlineBatchReceiverSoap;
import no.nav.altinnkanal.avro.NotifyTopicUpdate;
import no.nav.altinnkanal.services.TopicService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

@Component
public class UpdateTopicMappingCacheTask {
    private final Logger logger = LoggerFactory.getLogger(OnlineBatchReceiverSoap.class);

    private final Consumer<String, NotifyTopicUpdate> consumer;
    private final TopicService topicService;

    public UpdateTopicMappingCacheTask(Consumer<String, NotifyTopicUpdate> consumer, TopicService topicService) {
        this.consumer = consumer;
        this.topicService = topicService;
    }

    @Scheduled(fixedRate = 100)
    public void updateTopicMappings() throws Exception {

        logger.debug("Polling mapping update from Kafka");

        ConsumerRecords<String, NotifyTopicUpdate> records = consumer.poll(100);

        for (ConsumerRecord<String, NotifyTopicUpdate> record : records) {
            NotifyTopicUpdate value = record.value();

            logger.info("Updating topic mapping for service code: {} and service edition code {}",
                    keyValue("serviceCode", value.getServiceCode()),
                    keyValue("serviceEditionCode", value.getServiceEditionCode()));

            topicService.updateCache(value.getServiceCode().toString(), value.getServiceEditionCode().toString());
        }
    }
}
