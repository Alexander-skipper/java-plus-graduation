package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.event.EventInternalService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final EventInternalService eventInternalService;

    @PatchMapping("/{eventId}/increment-requests")
    @ResponseStatus(HttpStatus.OK)
    public void incrementConfirmedRequests(@PathVariable Long eventId) {
        log.info("Internal request: increment confirmed requests for event {}", eventId);
        eventInternalService.incrementConfirmedRequests(eventId);
    }

    @PatchMapping("/{eventId}/decrement-requests")
    @ResponseStatus(HttpStatus.OK)
    public void decrementConfirmedRequests(@PathVariable Long eventId) {
        log.info("Internal request: decrement confirmed requests for event {}", eventId);
        eventInternalService.decrementConfirmedRequests(eventId);
    }
}
