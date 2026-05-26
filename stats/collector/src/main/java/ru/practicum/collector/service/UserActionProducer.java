package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActionProducer {

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;

    @Value("${kafka.topics.user-actions:stats.user-actions.v1}")
    private String topic;

    public void send(UserActionAvro message) {
        log.info("Sending message to Kafka topic {}: userId={}, eventId={}",
                topic, message.getUserId(), message.getEventId());
        kafkaTemplate.send(topic, message.getUserId(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to Kafka topic {}: userId={}, eventId={}",
                                topic, message.getUserId(), message.getEventId(), ex);
                    } else {
                        log.debug("Successfully sent message to Kafka topic {}: userId={}, eventId={}, offset={}",
                                topic, message.getUserId(), message.getEventId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
