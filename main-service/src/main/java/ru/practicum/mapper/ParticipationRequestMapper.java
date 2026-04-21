package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requester.id")
    ParticipationRequestDto mapToDto(ParticipationRequest entity);

    @Mapping(target = "event", ignore = true)
    @Mapping(target = "requester", ignore = true)
    ParticipationRequest mapToEntity(ParticipationRequestDto dto);

}
