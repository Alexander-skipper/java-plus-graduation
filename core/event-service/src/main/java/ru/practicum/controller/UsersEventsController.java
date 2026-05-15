package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.*;
import ru.practicum.service.event.EventService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
public class UsersEventsController {
    private final EventService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto create(@PathVariable Long userId, @Valid @RequestBody NewEventRequestDto req) {
        log.info("POST /users/{}/events - запрос на создание события: {}", userId, req);
        return service.create(userId, req);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventResponseDto getUserEvent(@PathVariable Long userId,
                                         @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{} - запрос на получение события", userId, eventId);
        return service.getUserEvent(userId, eventId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> getUserEvents(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "0") int from,
                                              @RequestParam(defaultValue = "10") int size) {
        log.info("GET /users/{}/events - запрос на получение всех событий", userId);
        return service.getUserEvents(userId, PageRequest.of(from / size, size));
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventResponseDto update(@PathVariable Long userId,
                                   @PathVariable Long eventId,
                                   @Valid @RequestBody UpdateEventRequestDto req) {
        log.info("PATCH /users/{}/events/{} - запрос на обновление события {}", userId, eventId, req);
        return service.update(userId, eventId, req);
    }
}
