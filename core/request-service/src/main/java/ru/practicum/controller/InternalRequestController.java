package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.model.ParticipationRequest;
import org.springframework.web.bind.annotation.*;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.util.ParticipationRequestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class InternalRequestController {

    private final ParticipationRequestRepository requestRepository;

    @GetMapping("/count")
    public Long countByEventIdAndStatus(@RequestParam Long eventId,
                                        @RequestParam String status) {
        ParticipationRequestStatus requestStatus = ParticipationRequestStatus.valueOf(status);
        log.info("Internal request: count requests for event {} with status {}", eventId, requestStatus);
        return requestRepository.countByEventIdAndStatus(eventId, requestStatus);
    }

    @GetMapping("/count/batch")
    public Map<Long, Long> countByEventIdsAndStatus(@RequestParam List<Long> eventIds,
                                                    @RequestParam String status) {
        ParticipationRequestStatus requestStatus = ParticipationRequestStatus.valueOf(status);
        log.info("Internal request: batch count requests for {} events with status {}", eventIds.size(), requestStatus);

        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        return requestRepository.findAllByEventIdIn(eventIds).stream()
                .filter(r -> r.getStatus() == requestStatus)
                .collect(Collectors.groupingBy(
                        ParticipationRequest::getEventId,
                        Collectors.counting()
                ));
    }

    @GetMapping("/exists")
    public Boolean existsByUserIdAndEventIdAndStatus(@RequestParam Long userId,
                                                     @RequestParam Long eventId,
                                                     @RequestParam String status) {
        ParticipationRequestStatus requestStatus = ParticipationRequestStatus.valueOf(status);
        log.info("Internal request: check if user {} has {} request for event {}",
                userId, requestStatus, eventId);

        return requestRepository.findByRequesterIdAndEventId(userId, eventId)
                .map(request -> request.getStatus() == requestStatus)
                .orElse(false);
    }

}
