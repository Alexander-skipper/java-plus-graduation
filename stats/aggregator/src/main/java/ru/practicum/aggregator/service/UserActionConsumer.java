package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.model.WeightMatrix;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActionConsumer {

    private final SimilarityCalculator similarityCalculator;
    private final WeightMatrix weightMatrix;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity:stats.events-similarity.v1}")
    private String similarityTopic;

    @KafkaListener(topics = "${kafka.topics.user-actions:stats.user-actions.v1}",
            groupId = "aggregator-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(UserActionAvro userAction) {
        log.info("Received user action: userId={}, eventId={}, actionType={}, timestamp={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType(), userAction.getTimestamp());

        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();

        double oldWeight = similarityCalculator.getMaxWeightForEvent(eventId, userId);
        double newWeight = similarityCalculator.getWeightForAction(userAction.getActionType());

        log.debug("Old weight: {}, New weight: {}", oldWeight, newWeight);

        if (newWeight > oldWeight) {
            similarityCalculator.updateEventInteraction(eventId, userId, userAction.getActionType());
        } else {
            log.debug("No weight change for eventId={}, userId={}, skipping similarity update", eventId, userId);
            return;
        }

        Set<Long> allEventIds = weightMatrix.getEventWeightSums().keySet();
        Set<Long> eventsToUpdate = new HashSet<>(allEventIds);
        eventsToUpdate.remove(eventId);

        log.info("Recalculating similarity for eventId={} with {} other events", eventId, eventsToUpdate.size());

        for (Long otherEventId : eventsToUpdate) {
            double similarity = similarityCalculator.calculateAndSendSimilarity(eventId, otherEventId);

            if (similarity > 0) {
                EventSimilarityAvro similarityMessage = EventSimilarityAvro.newBuilder()
                        .setEventA(Math.min(eventId, otherEventId))
                        .setEventB(Math.max(eventId, otherEventId))
                        .setScore(similarity)
                        .setTimestamp(Instant.now())
                        .build();

                log.debug("Sending similarity for pair ({},{}): score={}",
                        similarityMessage.getEventA(), similarityMessage.getEventB(), similarity);

                kafkaTemplate.send(similarityTopic,
                        String.valueOf(similarityMessage.getEventA()),
                        similarityMessage);
            }
        }

        log.info("Finished processing user action for eventId={}", eventId);
    }
}
