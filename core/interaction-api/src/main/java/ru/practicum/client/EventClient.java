package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventResponseDto;
import ru.practicum.dto.event.ShortEventResponseDto;

import java.util.List;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/events/{eventId}")
    EventResponseDto getEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/users/{userId}/events/{eventId}")
    EventResponseDto getUserEvent(@PathVariable("userId") Long userId,
                                  @PathVariable("eventId") Long eventId);

    @GetMapping("/users/{userId}/events")
    List<ShortEventResponseDto> getUserEvents(@PathVariable("userId") Long userId,
                                              @RequestParam("from") int from,
                                              @RequestParam("size") int size);

    @PatchMapping("/events/{eventId}/increment")
    void incrementConfirmedRequests(@PathVariable("eventId") Long eventId);

    @PatchMapping("/events/{eventId}/decrement")
    void decrementConfirmedRequests(@PathVariable("eventId") Long eventId);
}
