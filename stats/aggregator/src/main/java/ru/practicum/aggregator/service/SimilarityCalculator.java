package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.model.WeightMatrix;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SimilarityCalculator {

    private final WeightMatrix weightMatrix;

    private static final Map<ActionTypeAvro, Double> ACTION_WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE, 1.0
    );

    public double getWeightForAction(ActionTypeAvro actionType) {
        return ACTION_WEIGHTS.getOrDefault(actionType, 0.0);
    }

    public double getMaxWeightForEvent(Long eventId, Long userId) {
        return weightMatrix.getWeight(eventId, userId);
    }

    public void updateEventInteraction(Long eventId, Long userId, ActionTypeAvro actionType) {
        double newWeight = getWeightForAction(actionType);
        double oldWeight = weightMatrix.getWeight(eventId, userId);

        if (newWeight <= oldWeight) {
            log.debug("No update needed: newWeight={} <= oldWeight={}", newWeight, oldWeight);
            return;
        }

        log.info("Updating interaction: eventId={}, userId={}, oldWeight={}, newWeight={}",
                eventId, userId, oldWeight, newWeight);

        double deltaWeight = newWeight - oldWeight;

        weightMatrix.putWeight(eventId, userId, newWeight);

        weightMatrix.updateEventWeightSum(eventId, deltaWeight);

        updateMinWeightSumsForUser(eventId, userId, oldWeight, newWeight);
    }

    private void updateMinWeightSumsForUser(Long eventA, Long userId, double oldWeightA, double newWeightA) {
        Map<Long, Double> userEvents = weightMatrix.getEventUserWeights().entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(userId))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(userId)
                ));

        for (Map.Entry<Long, Double> userEvent : userEvents.entrySet()) {
            Long eventB = userEvent.getKey();

            if (eventA.equals(eventB)) {
                continue;
            }

            Double weightB = userEvent.getValue();
            if (weightB == null || weightB == 0.0) {
                continue;
            }

            double oldMin = Math.min(oldWeightA, weightB);
            double newMin = Math.min(newWeightA, weightB);
            double deltaMin = newMin - oldMin;

            if (deltaMin != 0.0) {
                weightMatrix.updateMinWeightSum(eventA, eventB, deltaMin);
                log.debug("Updated S_min for pair ({},{}): delta={}, new S_min={}",
                        eventA, eventB, deltaMin, weightMatrix.getMinWeightSum(eventA, eventB));
            }
        }
    }

    public double calculateAndSendSimilarity(Long eventA, Long eventB) {
        return weightMatrix.getSimilarity(eventA, eventB);
    }
}
