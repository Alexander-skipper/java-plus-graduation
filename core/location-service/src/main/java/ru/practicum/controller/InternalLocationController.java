package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.location.LocationResponseDto;
import ru.practicum.dto.location.ShortLocationResponseDto;
import ru.practicum.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/internal/locations")
@RequiredArgsConstructor
@Slf4j
public class InternalLocationController {

    private final LocationService locationService;

    @GetMapping("/{locationId}")
    @ResponseStatus(HttpStatus.OK)
    public LocationResponseDto getLocation(@PathVariable Long locationId) {
        log.info("Internal request: get location by id {}", locationId);
        return locationService.findByIdFull(locationId);
    }

    @GetMapping("/near")
    @ResponseStatus(HttpStatus.OK)
    public List<ShortLocationResponseDto> findLocationsNear(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radius) {
        log.info("Internal request: find locations near lat={}, lon={}, radius={}", lat, lon, radius);

        return locationService.findLocationsNear(lat, lon, radius);
    }
}
