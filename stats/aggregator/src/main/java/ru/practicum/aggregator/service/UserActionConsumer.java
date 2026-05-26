package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.model.WeightMatrix;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
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
    public void consume(@Header(KafkaHeaders.RECEIVED_KEY) Long key, UserActionAvro userAction) {
        log.info("Received user action: userId={}, eventId={}, actionType={}, timestamp={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType(), userAction.getTimestamp());

        if (key != null && !key.equals(userAction.getUserId())) {
            log.warn("Key {} doesn't match userId {}", key, userAction.getUserId());
        }

        Long eventId = userAction.getEventId();
        Long userId = userAction.getUserId();

        double oldWeight = similarityCalculator.getMaxWeightForEvent(eventId, userId);
        double newWeight = similarityCalculator.getWeightForAction(userAction.getActionType());

        log.debug("Old weight: {}, New weight: {}", oldWeight, newWeight);

        if (newWeight <= oldWeight) {
            log.debug("No weight change for eventId={}, userId={}, skipping similarity update", eventId, userId);
            return;
        }

        Set<Long> userEventsBefore = getUserEventsForUser(userId);

        similarityCalculator.updateEventInteraction(eventId, userId, userAction.getActionType());

        Set<Long> userEventsAfter = new HashSet<>(userEventsBefore);
        userEventsAfter.add(eventId);

        log.info("Recalculating similarity for eventId={} with {} other events that user interacted with",
                eventId, userEventsAfter.size() - 1);

        Instant actionTimestamp = userAction.getTimestamp();

        for (Long otherEventId : userEventsAfter) {
            if (otherEventId.equals(eventId)) {
                continue;
            }

            Double weightOther = weightMatrix.getWeight(otherEventId, userId);
            if (weightOther == null || weightOther == 0.0) {
                continue;
            }

            double similarity = similarityCalculator.calculateAndSendSimilarity(eventId, otherEventId);

            if (similarity > 0) {
                EventSimilarityAvro similarityMessage = EventSimilarityAvro.newBuilder()
                        .setEventA(Math.min(eventId, otherEventId))
                        .setEventB(Math.max(eventId, otherEventId))
                        .setScore(similarity)
                        .setTimestamp(actionTimestamp)
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

    private Set<Long> getUserEventsForUser(Long userId) {
        Set<Long> events = new HashSet<>();
        Map<Long, Map<Long, Double>> eventUserWeights = weightMatrix.getEventUserWeights();

        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            if (entry.getValue().containsKey(userId)) {
                events.add(entry.getKey());
            }
        }
        return events;
    }
}
