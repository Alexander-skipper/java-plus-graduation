package ru.practicum.model;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.EndpointHitDto;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {
    @Mapping(target = "id", source = "endpointHit.id")
    @Mapping(target = "app", source = "endpointHit.app")
    @Mapping(target = "uri", source = "endpointHit.uri")
    @Mapping(target = "ip", source = "endpointHit.ip")
    @Mapping(target = "timestamp", source = "endpointHit.timestamp")
    EndpointHitDto mapToEndpointHitDto(EndpointHit endpointHit);

    @Mapping(target = "id", source = "endpointHitDto.id")
    @Mapping(target = "app", source = "endpointHitDto.app")
    @Mapping(target = "uri", source = "endpointHitDto.uri")
    @Mapping(target = "ip", source = "endpointHitDto.ip")
    @Mapping(target = "timestamp", source = "endpointHitDto.timestamp")
    EndpointHit mapToEndpointHit(EndpointHitDto endpointHitDto);
}
