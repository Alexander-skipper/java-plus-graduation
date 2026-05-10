package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventResponseDto;
import ru.practicum.service.event.EventInternalService;
import ru.practicum.service.event.EventService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final EventService eventService;
    private final EventInternalService eventInternalService;

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventResponseDto getEvent(@PathVariable Long eventId) {
        log.info("=== INTERNAL GET EVENT ===");
        log.info("getEvent() - internal request for eventId={}", eventId);

        boolean exists = eventService.existsById(eventId);
        log.info("getEvent() - event exists in DB: {}", exists);

        if (!exists) {
            log.error("getEvent() - Event with id {} NOT FOUND in database!", eventId);
            throw new NoSuchElementException("Event with id " + eventId + " not found");
        }

        EventResponseDto result = eventService.getEventById(eventId);
        log.info("getEvent() - returning event: id={}, title={}, state={}",
                result.getId(), result.getTitle(), result.getState());
        return result;
    }

    @PatchMapping("/{eventId}/increment-requests")
    @ResponseStatus(HttpStatus.OK)
    public void incrementConfirmedRequests(@PathVariable Long eventId) {
        log.info("=== INTERNAL INCREMENT REQUESTS ===");
        log.info("incrementConfirmedRequests() - internal request for eventId={}", eventId);
        eventInternalService.incrementConfirmedRequests(eventId);
        log.info("incrementConfirmedRequests() - completed for eventId={}", eventId);
    }

    @PatchMapping("/{eventId}/decrement-requests")
    @ResponseStatus(HttpStatus.OK)
    public void decrementConfirmedRequests(@PathVariable Long eventId) {
        log.info("=== INTERNAL DECREMENT REQUESTS ===");
        log.info("decrementConfirmedRequests() - internal request for eventId={}", eventId);
        eventInternalService.decrementConfirmedRequests(eventId);
        log.info("decrementConfirmedRequests() - completed for eventId={}", eventId);
    }

    @GetMapping("/{eventId}/exists")
    @ResponseStatus(HttpStatus.OK)
    public boolean existsById(@PathVariable Long eventId) {
        log.info("=== INTERNAL EXISTS CHECK ===");
        log.info("existsById() - checking if event {} exists", eventId);
        boolean exists = eventService.existsById(eventId);
        log.info("existsById() - event {} exists={}", eventId, exists);
        return exists;
    }
}