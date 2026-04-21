package ru.practicum.service.event;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.*;
import ru.practicum.util.EventState;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventResponseDto create(Long userId, NewEventRequestDto req);

    List<ShortEventResponseDto> find(EventSearchCriteria criteria) throws Exception;

    List<ShortEventResponseDto> getAll(Long userId, Pageable pageable);

    EventResponseDto get(Long eventId);

    EventResponseDto get(Long userId, Long eventId);

    EventResponseDto update(Long userId, Long eventId, UpdateEventRequestDto req);

    AdminEventResponseDto updateAdminEvent(Long eventId, UpdateEventAdminRequest req);

    List<AdminEventResponseDto> findAdminEvents(List<Long> users, List<EventState> states,
                                                List<Long> categories, LocalDateTime rangeStart,
                                                LocalDateTime rangeEnd, Pageable pageable);

    List<ShortEventResponseDto> findEventsByLocation(Long locationId, Pageable pageable);

    List<ShortEventResponseDto> findEventsNear(Double lat, Double lon, Double radius, Pageable pageable);

}
