package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.AdminEventResponseDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.service.event.EventService;
import ru.practicum.util.EventState;
import ru.practicum.util.Patterns;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
public class AdminEventsController {

    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AdminEventResponseDto> findAll(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = Patterns.TIMESTAMP_PATTERN)
            LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = Patterns.TIMESTAMP_PATTERN)
            LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("Search events with params: users={}, states={}, categories={}, rangeStart={}, rangeEnd={}, from={}, size={}",
                users, states, categories, rangeStart, rangeEnd, from, size);
        return eventService.findAdminEvents(users, states, categories, rangeStart, rangeEnd,
                PageRequest.of(from / size, size));
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public AdminEventResponseDto update(@PathVariable Long eventId,
                                        @RequestBody @Valid UpdateEventAdminRequest req) {
        log.info("Got request for Admin: update eventId {} with data {}", eventId, req);
        return eventService.updateAdminEvent(eventId, req);
    }
}