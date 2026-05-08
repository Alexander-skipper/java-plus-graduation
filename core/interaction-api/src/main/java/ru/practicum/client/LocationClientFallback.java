package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.location.ShortLocationResponseDto;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class LocationClientFallback implements LocationClient {

    @Override
    public ShortLocationResponseDto getLocation(Long locationId) {
        log.error("Location service is unavailable. Cannot get location by id {}.", locationId);
        throw new RuntimeException("Location service is unavailable");
    }

    @Override
    public List<ShortLocationResponseDto> findLocationsNear(Double lat, Double lon, Double radius) {
        log.warn("Location service is unavailable. Returning empty list for findLocationsNear");
        return Collections.emptyList();
    }

    @Override
    public Boolean locationExists(Long locationId) {
        log.error("Location service is unavailable. Cannot check location existence.");
        throw new RuntimeException("Location service is unavailable");
    }
}
