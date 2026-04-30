package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.StatsService;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class StatsController {

   private final StatsService statsService;

    @PostMapping ("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointHitDto addHit(@RequestBody EndpointHitDto endpointHitDto) {
        log.info("Received request to add hit {}", endpointHitDto);
        return statsService.addHit(endpointHitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatsDto> getStats(@RequestParam String start,
                                       @RequestParam String end,
                                       @RequestParam (required = false) List<String> uris,
                                       @RequestParam (required = false, defaultValue = "false") Boolean unique) {
        log.info("Received request to get stats {}", statsService);
        return
                statsService.getStats(start, end, uris, unique);
    }

}
