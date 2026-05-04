package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "requester", source = "requesterId")
    ParticipationRequestDto mapToDto(ParticipationRequest entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", source = "event")
    @Mapping(target = "requesterId", source = "requester")
    ParticipationRequest mapToEntity(ParticipationRequestDto dto);

}
