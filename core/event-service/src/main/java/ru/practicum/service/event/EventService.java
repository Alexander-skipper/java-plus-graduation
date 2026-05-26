package ru.practicum.service.event;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.event.*;
import ru.practicum.util.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface EventService {

    EventResponseDto create(Long userId, NewEventRequestDto req);
    EventResponseDto update(Long userId, Long eventId, UpdateEventRequestDto req);
    List<ShortEventResponseDto> getUserEvents(Long userId, Pageable pageable);
    EventResponseDto getUserEvent(Long userId, Long eventId);

    List<ShortEventResponseDto> find(EventSearchCriteria criteria);
    EventResponseDto getPublicEvent(Long eventId, Long userId);
    List<ShortEventResponseDto> findEventsByLocation(Long locationId, Pageable pageable);
    List<ShortEventResponseDto> findEventsNear(Double lat, Double lon, Double radius, Pageable pageable);

    AdminEventResponseDto updateAdminEvent(Long eventId, UpdateEventAdminRequest req);
    List<AdminEventResponseDto> findAdminEvents(List<Long> users, List<EventState> states,
                                                List<Long> categories, LocalDateTime rangeStart,
                                                LocalDateTime rangeEnd, Pageable pageable);

    EventResponseDto getInternalEventById(Long eventId);
    boolean existsById(Long eventId);
    Map<Long, Integer> getConfirmedRequestsCounts(List<Long> eventIds);

    List<ShortEventResponseDto> getRecommendationsForUser(Long userId, int maxResults);
    void likeEvent(Long userId, Long eventId);
    void updateEventRating(Long eventId);
}