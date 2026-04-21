package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.event.*;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.User;

@Mapper(componentModel = "spring",
        uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lat", source = "newEventRequest.location.lat")
    @Mapping(target = "lon", source = "newEventRequest.location.lon")
    @Mapping(target = "state", expression = "java(ru.practicum.util.EventState.PENDING)")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "user")
    Event eventRequestToEvent(NewEventRequestDto newEventRequest, Category category, User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "category", source = "category")
    Event updateEventField(@MappingTarget Event event, UpdateEventRequestDto req, Category category);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "location.lat", source = "event.lat")
    @Mapping(target = "location.lon", source = "event.lon")
    @Mapping(target = "initiator", source = "user")
    @Mapping(target = "initiator.id", source = "user.id")
    @Mapping(target = "initiator.name", source = "user.name")
    ShortEventResponseDto eventToShortEventResponseDto(Event event, User user);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "location.lat", source = "event.lat")
    @Mapping(target = "location.lon", source = "event.lon")
    @Mapping(target = "initiator", source = "event.initiator")
    ShortEventResponseDto eventToShortEventResponseDto(Event event);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "location.lat", source = "event.lat")
    @Mapping(target = "location.lon", source = "event.lon")
    @Mapping(target = "initiator", source = "user")
    @Mapping(target = "initiator.id", source = "user.id")
    @Mapping(target = "initiator.name", source = "user.name")
    EventResponseDto eventToEventResponseDto(Event event, User user);

    @Mapping(target = "initiator", source = "event.initiator")
    AdminEventResponseDto toAdminEventFullDto(Event event);
}
