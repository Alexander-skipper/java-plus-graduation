package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.Category;
import ru.practicum.model.Event;

@Mapper(componentModel = "spring",
        uses = {CategoryMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lat", source = "newEventRequest.location.lat")
    @Mapping(target = "lon", source = "newEventRequest.location.lon")
    @Mapping(target = "state", expression = "java(ru.practicum.util.EventState.PENDING)")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiatorId", source = "user.id")
    @Mapping(target = "confirmedRequests", constant = "0")
    Event eventRequestToEvent(NewEventRequestDto newEventRequest, Category category, UserDto user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", source = "category")
    Event updateEventField(@MappingTarget Event event, UpdateEventRequestDto req, Category category);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "location.lat", source = "event.lat")
    @Mapping(target = "location.lon", source = "event.lon")
    @Mapping(target = "initiator", source = "userShort")
    @Mapping(target = "views", ignore = true)
    ShortEventResponseDto eventToShortEventResponseDto(Event event, UserShortDto userShort);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "location.lat", source = "event.lat")
    @Mapping(target = "location.lon", source = "event.lon")
    @Mapping(target = "initiator", source = "userShort")
    @Mapping(target = "views", ignore = true)
    EventResponseDto eventToEventResponseDto(Event event, UserShortDto userShort);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "initiator", source = "userShort")
    @Mapping(target = "location.lat", source = "event.lat")
    @Mapping(target = "location.lon", source = "event.lon")
    @Mapping(target = "views", ignore = true)
    AdminEventResponseDto toAdminEventFullDto(Event event, UserShortDto userShort);
}
