package ru.practicum.aggregator.model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@Getter
public class WeightMatrix {

    @Getter
    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightSums = new ConcurrentHashMap<>();

    public void putWeight(Long eventId, Long userId, Double weight) {
        eventUserWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>()).put(userId, weight);
        log.debug("Updated weight: eventId={}, userId={}, weight={}", eventId, userId, weight);
    }

    public Double getWeight(Long eventId, Long userId) {
        Map<Long, Double> users = eventUserWeights.get(eventId);
        if (users == null) {
            return 0.0;
        }
        return users.getOrDefault(userId, 0.0);
    }

    public void updateEventWeightSum(Long eventId, Double delta) {
        eventWeightSums.merge(eventId, delta, Double::sum);
        log.debug("Updated sum for eventId={}, new sum={}", eventId, eventWeightSums.get(eventId));
    }

    public Double getEventWeightSum(Long eventId) {
        return eventWeightSums.getOrDefault(eventId, 0.0);
    }

    public void updateMinWeightSum(Long eventA, Long eventB, Double delta) {
        Long first = Math.min(eventA, eventB);
        Long second = Math.max(eventA, eventB);
        minWeightSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
        log.debug("Updated S_min for pair ({},{}), new value={}", first, second, getMinWeightSum(eventA, eventB));
    }

    public Double getMinWeightSum(Long eventA, Long eventB) {
        Long first = Math.min(eventA, eventB);
        Long second = Math.max(eventA, eventB);
        Map<Long, Double> inner = minWeightSums.get(first);
        if (inner == null) {
            return 0.0;
        }
        return inner.getOrDefault(second, 0.0);
    }

    public Double getSimilarity(Long eventA, Long eventB) {
        Double sMin = getMinWeightSum(eventA, eventB);
        Double sA = getEventWeightSum(eventA);
        Double sB = getEventWeightSum(eventB);

        if (sMin == 0.0) {
            return 0.0;
        }

        if (sA == 0.0 || sB == 0.0) {
            return 0.0;
        }

        double denominator = Math.sqrt(sA) * Math.sqrt(sB);
        if (denominator == 0.0) {
            return 0.0;
        }

        double similarity = sMin / denominator;

        return Math.round(similarity * 100.0) / 100.0;
    }
}
