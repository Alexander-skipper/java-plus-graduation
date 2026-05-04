package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventResponseDto;
import ru.practicum.dto.event.ShortEventResponseDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class EventClientFallback implements EventClient {

    @Override
    public EventResponseDto getEvent(Long eventId) {
        log.warn("Event-service unavailable or eventId not found, returning fallback for eventId: {}", eventId);

        return EventResponseDto.builder()
                .id(eventId)
                .title("Unknown Event")
                .annotation("Event data temporarily unavailable")
                .build();
    }

    @Override
    public EventResponseDto getUserEvent(Long userId, Long eventId) {
        log.warn("Event-service unavailable, returning fallback for userId: {}, eventId: {}", userId, eventId);

        return EventResponseDto.builder()
                .id(eventId)
                .title("Unknown Event")
                .annotation("Event data temporarily unavailable")
                .build();
    }

    @Override
    public List<ShortEventResponseDto> getUserEvents(Long userId, int from, int size) {
        log.warn("Event-service unavailable, returning empty list for userId: {}", userId);
        return Collections.emptyList();
    }

    @Override
    public void incrementConfirmedRequests(Long eventId) {
        throw new RuntimeException("Event service is unavailable");
    }

    @Override
    public void decrementConfirmedRequests(Long eventId) {
        throw new RuntimeException("Event service is unavailable");
    }
}
