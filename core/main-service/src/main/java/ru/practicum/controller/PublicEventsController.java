package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.event.EventResponseDto;
import ru.practicum.dto.event.EventSearchCriteria;
import ru.practicum.dto.event.ShortEventResponseDto;
import ru.practicum.service.event.EventService;
import ru.practicum.stats.client.StatsClient;


import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventsController {

    private static final String MAIN_SERVICE = "ewm-main-service";

    private final EventService service;
    private final StatsClient statsClient;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> findAll(@ModelAttribute EventSearchCriteria criteria, HttpServletRequest req) throws Exception {
       log.info("Find all events");
       List<ShortEventResponseDto> res = service.find(criteria);
       saveHit(req);
       return res;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventResponseDto getEvent(@PathVariable Long id, HttpServletRequest req) {
        log.info("Get event by id {}", id);
        EventResponseDto res = service.get(id);
        saveHit(req);
        return res;
    }

    //!!!!!!!!FEATURE - 3 ЗАДАНИЕ
    @GetMapping("/location/{locationId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ShortEventResponseDto> findEventsByLocation(@PathVariable Long locationId,
                                                      @RequestParam(defaultValue = "0") Integer from,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      HttpServletRequest req) {

        log.info("Find events by location {}", locationId);
        saveHit(req);
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


    private void saveHit(HttpServletRequest request) {
        EndpointHitDto endpointHitDto = new EndpointHitDto();
        endpointHitDto.setApp(MAIN_SERVICE);
        endpointHitDto.setUri(request.getRequestURI());
        endpointHitDto.setIp(request.getRemoteAddr());
        endpointHitDto.setTimestamp(LocalDateTime.now());
        statsClient.hit(endpointHitDto);
    }
}
