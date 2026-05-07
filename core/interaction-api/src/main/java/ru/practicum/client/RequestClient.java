package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient {

    @PostMapping("/users/{userId}/requests")
    ParticipationRequestDto createRequest(@PathVariable Long userId,
                                          @RequestParam Long eventId);

    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> getRequests(@PathVariable Long userId);

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                          @PathVariable Long requestId);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                   @PathVariable Long eventId);

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    EventRequestStatusUpdateResult updateRequestStatus(@PathVariable Long userId,
                                                       @PathVariable Long eventId,
                                                       @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("/internal/requests/count")
    Long countByEventIdAndStatus(@RequestParam Long eventId,
                                 @RequestParam String status);
}
