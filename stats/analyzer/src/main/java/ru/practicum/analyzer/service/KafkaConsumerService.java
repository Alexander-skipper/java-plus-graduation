package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.config.ActionWeightsConfig;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.model.UserActionEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;
    private final ActionWeightsConfig actionWeightsConfig;

    @KafkaListener(topics = "${kafka.topics.user-actions:stats.user-actions.v1}",
            groupId = "analyzer-group",
            containerFactory = "userActionKafkaListenerContainerFactory")
    @Transactional
    public void consumeUserAction(@Header(KafkaHeaders.RECEIVED_KEY) Long key, UserActionAvro userAction) {
        log.info("Consuming user action: userId={}, eventId={}, actionType={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

        if (key != null && !key.equals(userAction.getUserId())) {
            log.warn("Key mismatch! Key: {}, UserId: {}", key, userAction.getUserId());
        }

        double weight = getWeight(userAction.getActionType());

        Optional<UserActionEntity> existingOpt = userActionRepository
                .findByUserIdAndEventId(userAction.getUserId(), userAction.getEventId());

        LocalDateTime actionTime = LocalDateTime.ofInstant(userAction.getTimestamp(), ZoneId.systemDefault());

        if (existingOpt.isPresent()) {
            UserActionEntity existing = existingOpt.get();
            existing.setLastActionTime(actionTime);
            double newWeight = Math.max(existing.getWeight(), weight);

            existing.setWeight(newWeight);
            existing.setActionType(userAction.getActionType().toString());

            log.info("Updated user action: userId={}, eventId={}, addedWeight={}, oldWeight={}, newWeight={}, newTime={}",
                    userAction.getUserId(), userAction.getEventId(), weight,
                    newWeight - weight, newWeight, actionTime);

            userActionRepository.save(existing);
        } else {
            UserActionEntity entity = UserActionEntity.builder()
                    .userId(userAction.getUserId())
                    .eventId(userAction.getEventId())
                    .weight(weight)
                    .lastActionTime(actionTime)
                    .actionType(userAction.getActionType().toString())
                    .build();
            userActionRepository.save(entity);
            log.info("Saved new user action: userId={}, eventId={}, weight={}, time={}",
                    userAction.getUserId(), userAction.getEventId(), weight, actionTime);
        }
    }

    @KafkaListener(topics = "${kafka.topics.events-similarity:stats.events-similarity.v1}",
            groupId = "analyzer-group",
            containerFactory = "similarityKafkaListenerContainerFactory")
    @Transactional
    public void consumeEventSimilarity(EventSimilarityAvro similarity) {
        log.info("Consuming event similarity: eventA={}, eventB={}, score={}",
                similarity.getEventA(), similarity.getEventB(), similarity.getScore());

        Optional<EventSimilarityEntity> existingOpt = eventSimilarityRepository
                .findByEventAAndEventB(similarity.getEventA(), similarity.getEventB());

        if (existingOpt.isPresent()) {
            EventSimilarityEntity existing = existingOpt.get();
            existing.setScore(similarity.getScore());
            existing.setUpdatedAt(LocalDateTime.now());
            eventSimilarityRepository.save(existing);
            log.info("Updated similarity for pair ({},{}): new score={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        } else {
            EventSimilarityEntity entity = EventSimilarityEntity.builder()
                    .eventA(similarity.getEventA())
                    .eventB(similarity.getEventB())
                    .score(similarity.getScore())
                    .updatedAt(LocalDateTime.now())
                    .build();
            eventSimilarityRepository.save(entity);
            log.info("Saved new similarity for pair ({},{}): score={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        }
    }

    private double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> actionWeightsConfig.getView();
            case REGISTER -> actionWeightsConfig.getRegister();
            case LIKE -> actionWeightsConfig.getLike();
        };
    }
}
