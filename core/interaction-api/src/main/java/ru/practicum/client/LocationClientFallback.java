package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.location.LocationResponseDto;
import ru.practicum.dto.location.ShortLocationResponseDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class LocationClientFallback implements LocationClient {

    @Override
    public LocationResponseDto getLocation(Long locationId) {
        log.warn("Location-service unavailable, returning fallback for locationId: {}", locationId);

        LocationResponseDto fallback = new LocationResponseDto();
        fallback.setId(locationId);
        fallback.setName("Unknown Location");
        fallback.setLatitude(0.0);
        fallback.setLongitude(0.0);
        fallback.setRadius(1.0);
        return fallback;
    }

    @Override
    public List<ShortLocationResponseDto> findLocationsNear(Double lat, Double lon, Double radius) {
        log.warn("Location-service unavailable, returning empty list for coordinates: lat={}, lon={}", lat, lon);

        return Collections.emptyList();
    }
}
