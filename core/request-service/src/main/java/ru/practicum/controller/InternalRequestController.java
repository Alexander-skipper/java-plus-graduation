package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.util.ParticipationRequestStatus;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class InternalRequestController {

    private final ParticipationRequestRepository requestRepository;

    @GetMapping("/count")
    @ResponseStatus(HttpStatus.OK)
    public Long countByEventIdAndStatus(@RequestParam Long eventId,
                                        @RequestParam String status) {
        ParticipationRequestStatus requestStatus = ParticipationRequestStatus.valueOf(status);
        log.info("Internal request: count requests for event {} with status {}", eventId, requestStatus);
        return requestRepository.countByEventIdAndStatus(eventId, requestStatus);
    }
}
