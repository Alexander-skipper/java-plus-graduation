package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarityEntity;
import ru.practicum.analyzer.model.UserActionEntity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    private static final int DEFAULT_NEIGHBORS_COUNT = 10;

    public List<RecommendedEvent> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Getting recommendations for user: {}, maxResults={}", userId, maxResults);

        List<Long> userEventIds = userActionRepository.findEventIdsByUserId(userId);
        if (userEventIds.isEmpty()) {
            log.info("User {} has no interactions, returning empty list", userId);
            return Collections.emptyList();
        }

        List<UserActionEntity> userActions = userActionRepository.findAllByUserId(userId);
        userActions.sort((a, b) -> b.getLastActionTime().compareTo(a.getLastActionTime()));
        List<Long> recentEventIds = userActions.stream()
                .limit(DEFAULT_NEIGHBORS_COUNT)
                .map(UserActionEntity::getEventId)
                .collect(Collectors.toList());

        log.info("Found {} recent events for user {}", recentEventIds.size(), userId);

        Map<Long, Double> candidateScores = new HashMap<>();

        for (Long eventId : recentEventIds) {
            List<EventSimilarityEntity> similarEvents = eventSimilarityRepository
                    .findTopSimilarForEvent(eventId, PageRequest.of(0, maxResults * 2));

            for (EventSimilarityEntity similar : similarEvents) {
                Long candidateId = similar.getEventA().equals(eventId) ? similar.getEventB() : similar.getEventA();

                if (!userEventIds.contains(candidateId)) {
                    candidateScores.merge(candidateId, similar.getScore(), Double::sum);
                }
            }
        }

        List<RecommendedEvent> recommendations = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> new RecommendedEvent(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        log.info("Generated {} recommendations for user {}", recommendations.size(), userId);
        return recommendations;
    }

    public List<RecommendedEvent> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        log.info("Getting similar events for eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);

        Set<Long> userEventIds = new HashSet<>();
        if (userId != null && userId > 0) {
            userEventIds = new HashSet<>(userActionRepository.findEventIdsByUserId(userId));
        }

        List<EventSimilarityEntity> similarEvents = eventSimilarityRepository
                .findTopSimilarForEvent(eventId, PageRequest.of(0, maxResults * 2));

        List<RecommendedEvent> recommendations = new ArrayList<>();

        for (EventSimilarityEntity sim : similarEvents) {
            Long otherId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();

            if (!userEventIds.contains(otherId)) {
                recommendations.add(new RecommendedEvent(otherId, sim.getScore()));
            }

            if (recommendations.size() >= maxResults) {
                break;
            }
        }

        log.info("Found {} similar events for eventId={}", recommendations.size(), eventId);
        return recommendations;
    }

    public List<RecommendedEvent> getInteractionsCount(List<Long> eventIds) {
        log.info("Getting interactions count for {} events", eventIds.size());

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object[]> results = userActionRepository.sumWeightsByEventIds(eventIds);

        Map<Long, Double> eventWeightSums = new HashMap<>();

        for (Object[] row : results) {
            Long eventId = (Long) row[0];
            Double weightSum = ((Number) row[1]).doubleValue();
            eventWeightSums.put(eventId, weightSum);
        }

        for (Long eventId : eventIds) {
            eventWeightSums.putIfAbsent(eventId, 0.0);
        }


        return eventWeightSums.entrySet().stream()
                .map(entry -> new RecommendedEvent(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public record RecommendedEvent(Long eventId, Double score) {}
}
