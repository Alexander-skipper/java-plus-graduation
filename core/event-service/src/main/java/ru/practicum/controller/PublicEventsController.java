package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventResponseDto;
import ru.practicum.dto.event.EventSearchCriteria;
import ru.practicum.dto.event.ShortEventResponseDto;
import ru.practicum.service.event.EventService;


import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventsController {

    private final EventService service;

    @Value("${stats.service.app-name:event-service}")
    private String appName;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> findAll(@ModelAttribute EventSearchCriteria criteria, HttpServletRequest req) {
       log.info("Find all events with sort: {}", criteria.getSort());
       List<ShortEventResponseDto> res = service.find(criteria);
       return res;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventResponseDto getEvent(@PathVariable Long id,
                                     @RequestHeader(value = "X-EWM-USER-ID", required = false)Long userId,
                                     HttpServletRequest req) {
        log.info("Get eventId by id {}, userId={}", id, userId);

        return service.getPublicEvent(id, userId);
    }

    @GetMapping("/recommendations")
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId,
                                                          @RequestParam(defaultValue = "10") int maxResults) {
        log.info("Get recommendations for user {}, maxResults={}", userId, maxResults);
        return service.getRecommendationsForUser(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("User {} likes event {}", userId, eventId);
        service.likeEvent(userId, eventId);
    }

    //!!!!!!!!FEATURE - 3 ЗАДАНИЕ
    @GetMapping("/location/{locationId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> findEventsByLocation(@PathVariable Long locationId,
                                                      @RequestParam(defaultValue = "0") Integer from,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      HttpServletRequest req) {

        log.info("Find events by location {}", locationId);
        return service.findEventsByLocation(locationId, PageRequest.of(from / size, size, Sort.by("event_date").descending()));

    }

    @GetMapping("/near")
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> findEventsNear(@RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") Double lat,
                                                      @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") Double lon,
                                                      @RequestParam(defaultValue = "1.0") @DecimalMin("0.1") Double radius,
                                                      @RequestParam(defaultValue = "0") Integer from,
                                                      @RequestParam(defaultValue = "10") Integer size) {

        log.info("Find events in locations where user is located: lat={}, lon={}, from={}, size={}",
                lat, lon, from, size);

        return service.findEventsNear(lat, lon, radius,
                PageRequest.of(from / size, size, Sort.by("event_date").descending()));
    }
    //!!!!!!!!FEATURE - 3 ЗАДАНИЕ

}
