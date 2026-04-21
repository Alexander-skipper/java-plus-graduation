package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.event.EventService;
import ru.practicum.service.request.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Valid
public class UsersEventsController {
    private final EventService service;
    private final ParticipationRequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseDto create(@PathVariable Long userId, @Valid @RequestBody NewEventRequestDto req) {
        log.info("POST /users/{}/events - запрос на создание события: {}", userId, req);
        return service.create(userId, req);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventResponseDto get(@PathVariable Long userId,
                                @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{} - запрос на получение события", userId, eventId);
        return service.get(userId, eventId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> getAll(@PathVariable Long userId,
                                              @RequestParam(defaultValue = "0") int from,
                                              @RequestParam(defaultValue = "10") int size) {
        log.info("GET /users/{}/events - запрос на получение всех событий", userId);
        return service.getAll(userId, PageRequest.of(from / size, size));
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventResponseDto update(@PathVariable Long userId,
                                   @PathVariable Long eventId,
                                   @Valid @RequestBody UpdateEventRequestDto req) {
        log.info("PATCH /users/{}/events/{} - запрос на обновление события {}", userId, eventId, req);
        return service.update(userId, eventId, req);
    }

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getRequests(@PathVariable Long userId,
                                                     @PathVariable Long eventId) {
        log.info("Получение информации о запросах на участие в событии текущего пользователя");
        return requestService.getUsersRequestsForUserEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult getRequests(@PathVariable Long userId,
                                                      @PathVariable Long eventId,
                                                      @RequestBody EventRequestStatusUpdateRequest req) {
        log.info("Новый статус для заявок на участие в событии {} пользователя {}",eventId, userId);
        return requestService.updateRequestStatus(userId, eventId, req);
    }

}
