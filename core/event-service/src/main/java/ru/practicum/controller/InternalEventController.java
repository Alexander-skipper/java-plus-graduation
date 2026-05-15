package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventResponseDto;
import ru.practicum.service.event.EventService;


@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventResponseDto getEvent(@PathVariable Long eventId) {
        log.info("=== INTERNAL GET EVENT ===");
        log.info("Internal request for eventId={}", eventId);
        return eventService.getInternalEventById(eventId);
    }

    @GetMapping("/{eventId}/exists")
    public boolean existsById(@PathVariable Long eventId) {
        log.info("=== INTERNAL EXISTS CHECK ===");
        log.info("Internal checking if event {} exists", eventId);
        return eventService.existsById(eventId);
    }
}