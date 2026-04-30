package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.location.LocationResponseDto;
import ru.practicum.dto.location.NewLocationDto;
import ru.practicum.dto.location.UpdateLocationDto;
import ru.practicum.service.location.LocationService;

import java.util.List;

@RestController
@RequestMapping("/admin/location")
@RequiredArgsConstructor
@Slf4j
public class AdminLocationController {

    private final LocationService locationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<LocationResponseDto> findAllFull(@RequestParam(defaultValue = "0") Integer from,
                                             @RequestParam(defaultValue = "10") Integer size) {
        log.info("Find all locations");
        return locationService.findAllFull(PageRequest.of(from / size, size, Sort.by("id").ascending()));
    }

    @GetMapping("/{locationId}")
    @ResponseStatus(HttpStatus.OK)
    public LocationResponseDto findByIdFull(@PathVariable Long locationId) {
        log.info("Find location with id {}", locationId);
        return locationService.findByIdFull(locationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationResponseDto create(@RequestBody @Valid NewLocationDto dto) {
        log.info("Create location {}", dto);
        return locationService.create(dto);
    }

    @PatchMapping("/{locationId}")
    @ResponseStatus(HttpStatus.OK)
    public LocationResponseDto update(@PathVariable Long locationId,
                                      @Valid @RequestBody UpdateLocationDto dto) {
        log.info("Update location {}", dto);
        return locationService.update(locationId, dto);
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long locationId) {
        log.info("Delete location {}", locationId);
        locationService.delete(locationId);
    }

}
