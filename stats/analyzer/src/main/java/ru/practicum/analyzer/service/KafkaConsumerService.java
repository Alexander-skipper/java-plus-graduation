package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.model.UserActionEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    @KafkaListener(topics = "${kafka.topics.user-actions:stats.user-actions.v1}",
            groupId = "analyzer-group",
            containerFactory = "userActionKafkaListenerContainerFactory")
    @Transactional
    public void consumeUserAction(UserActionAvro userAction) {
        log.info("Consuming user action: userId={}, eventId={}, actionType={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

        double weight = getWeight(userAction.getActionType());

        Optional<UserActionEntity> existingOpt = userActionRepository
                .findByUserIdAndEventId(userAction.getUserId(), userAction.getEventId());

        if (existingOpt.isPresent()) {
            UserActionEntity existing = existingOpt.get();
            if (weight > existing.getWeight()) {
                existing.setWeight(weight);
                existing.setLastActionTime(LocalDateTime.now());
                existing.setActionType(userAction.getActionType().toString());
                userActionRepository.save(existing);
                log.info("Updated user action: userId={}, eventId={}, new weight={}",
                        userAction.getUserId(), userAction.getEventId(), weight);
            } else {
                log.debug("Skipped update: existing weight {} >= new weight {}", existing.getWeight(), weight);
            }
        } else {
            UserActionEntity entity = UserActionEntity.builder()
                    .userId(userAction.getUserId())
                    .eventId(userAction.getEventId())
                    .weight(weight)
                    .lastActionTime(LocalDateTime.now())
                    .actionType(userAction.getActionType().toString())
                    .build();
            userActionRepository.save(entity);
            log.info("Saved new user action: userId={}, eventId={}, weight={}",
                    userAction.getUserId(), userAction.getEventId(), weight);
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
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }
}
