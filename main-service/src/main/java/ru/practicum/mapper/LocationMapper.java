package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.location.LocationResponseDto;
import ru.practicum.dto.location.NewLocationDto;
import ru.practicum.dto.location.ShortLocationResponseDto;
import ru.practicum.dto.location.UpdateLocationDto;
import ru.practicum.model.Location;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LocationMapper {

    LocationResponseDto toFullResponseDto(Location location);

    ShortLocationResponseDto toShortResponseDto(Location location);

    @Mapping(target = "id", ignore = true)
    Location toLocation(NewLocationDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, ignoreByDefault = false)
    void updateFromDto(UpdateLocationDto dto, @MappingTarget Location entity);
}
