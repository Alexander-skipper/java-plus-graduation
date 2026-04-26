package ru.practicum.service.location;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.location.LocationResponseDto;
import ru.practicum.dto.location.NewLocationDto;
import ru.practicum.dto.location.ShortLocationResponseDto;
import ru.practicum.dto.location.UpdateLocationDto;

import java.util.List;

public interface LocationService {

    List<LocationResponseDto> findAllFull(Pageable pageable);

    LocationResponseDto findByIdFull(Long locationId);

    List<ShortLocationResponseDto> findAllShort(Pageable pageable);

    ShortLocationResponseDto findByIdShort(Long locationId);

    LocationResponseDto create(NewLocationDto dto);

    LocationResponseDto update(Long locationId, UpdateLocationDto dto);

    void delete(Long locationId);
}
