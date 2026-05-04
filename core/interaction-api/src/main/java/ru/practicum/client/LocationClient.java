package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.location.LocationResponseDto;
import ru.practicum.dto.location.ShortLocationResponseDto;

import java.util.List;

@FeignClient(
        name = "location-service",
        fallback = LocationClientFallback.class
)
public interface LocationClient {

    @GetMapping("/internal/locations/{locationId}")
    LocationResponseDto getLocation(@PathVariable("locationId") Long locationId);

    @GetMapping("/internal/locations/near")
    List<ShortLocationResponseDto> findLocationsNear(
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon,
            @RequestParam("radius") Double radius
    );
}
