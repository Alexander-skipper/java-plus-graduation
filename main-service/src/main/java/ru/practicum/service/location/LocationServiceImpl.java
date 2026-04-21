package ru.practicum.service.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.location.LocationResponseDto;
import ru.practicum.dto.location.NewLocationDto;
import ru.practicum.dto.location.ShortLocationResponseDto;
import ru.practicum.dto.location.UpdateLocationDto;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Location;
import ru.practicum.repository.LocationRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository repository;
    private final LocationMapper mapper;

    @Override
    public List<LocationResponseDto> findAllFull(Pageable pageable) {
        log.info("Find all locations - returns full information");
        return repository.findAll(pageable).stream()
                .map(mapper::toFullResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public LocationResponseDto findByIdFull(Long locationId) {
        log.info("Find location by id {} - returns full information", locationId);
        Location location = repository.findById(locationId)
                .orElseThrow(() -> {
            return new NoSuchElementException("Location with id " + locationId + " notFound");
        });
        return mapper.toFullResponseDto(location);
    }

    @Override
    public List<ShortLocationResponseDto> findAllShort(Pageable pageable) {
        log.info("Find all locations - returns short information");
        return repository.findAll(pageable).stream()
                .map(mapper::toShortResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ShortLocationResponseDto findByIdShort(Long locationId) {
        log.info("Find location by id {} - returns short information", locationId);
        Location location = repository.findById(locationId)
                .orElseThrow(() -> {
                    return new NoSuchElementException("Location with id " + locationId + " notFound");
                });
        return mapper.toShortResponseDto(location);
    }

    @Override
    @Transactional
    public LocationResponseDto create(NewLocationDto dto) {
        log.info("Create new location {}", dto);
        Location location = mapper.toLocation(dto);
        return mapper.toFullResponseDto(repository.save(location));
    }

    @Override
    @Transactional
    public LocationResponseDto update(Long locationId, UpdateLocationDto dto) {
        log.info("Update location {}", dto);
        Location existingLocation = repository.findById(locationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Location with id=" + locationId + " not found"));
        mapper.updateFromDto(dto, existingLocation);
        return mapper.toFullResponseDto(existingLocation);
    }

    @Override
    @Transactional
    public void delete(Long locationId) {
        log.info("Delete location {}", locationId);
        Location location = repository.findById(locationId)
                .orElseThrow(() -> {
                    return new NoSuchElementException("Location with id " + locationId + " notFound");
                });
        repository.deleteById(locationId);
    }
}
