package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.location.ShortLocationResponseDto;
import ru.practicum.service.location.LocationService;

import java.util.List;

@RestController
@RequestMapping("/public/location")
@RequiredArgsConstructor
@Slf4j
public class PublicLocationController {

    private final LocationService locationService;

    @GetMapping("/{locationId}")
    @ResponseStatus(HttpStatus.OK)
    public ShortLocationResponseDto findByIdShort(@PathVariable Long locationId) {
        log.info("Find location with id {}", locationId);
        return locationService.findByIdShort(locationId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ShortLocationResponseDto> findAllShort(@RequestParam(defaultValue = "0") Integer from,
                                                       @RequestParam(defaultValue = "10") Integer size) {
        log.info("Find all locations");
        return locationService.findAllShort(PageRequest.of(from / size, size, Sort.by("id").ascending()));
    }

}
