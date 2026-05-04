package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "request-service",
        fallback = RequestClientFallback.class
)
public interface RequestClient {

    @GetMapping("/internal/requests/count/{eventId}")
    Long getConfirmedRequestsCount(@PathVariable("eventId") Long eventId);

    @PatchMapping("/internal/requests/eventId/{eventId}/increment")
    void incrementConfirmedRequests(@PathVariable("eventId") Long eventId);

    @PatchMapping("/internal/requests/eventId/{eventId}/decrement")
    void decrementConfirmedRequests(@PathVariable("eventId") Long eventId);
}
