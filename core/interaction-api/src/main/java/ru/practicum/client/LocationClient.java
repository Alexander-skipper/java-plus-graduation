package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.location.ShortLocationResponseDto;

import java.util.List;

@FeignClient(name = "location-service", fallback = LocationClientFallback.class)
public interface LocationClient {

    @GetMapping("/internal/locations/{locationId}")
    ShortLocationResponseDto getLocation(@PathVariable Long locationId);

    @GetMapping("/internal/locations/near")
    List<ShortLocationResponseDto> findLocationsNear(@RequestParam Double lat,
                                                     @RequestParam Double lon,
                                                     @RequestParam Double radius);

    @GetMapping("/internal/locations/{locationId}/exists")
    Boolean locationExists(@PathVariable Long locationId);
}
