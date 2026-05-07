package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.event.EventResponseDto;

import java.util.List;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventResponseDto getEvent(@PathVariable Long eventId);

    @GetMapping("/internal/events/{eventId}/exists")
    Boolean eventExists(@PathVariable Long eventId);

    @PatchMapping("/internal/events/{eventId}/increment-requests")
    void incrementConfirmedRequests(@PathVariable Long eventId);

    @PatchMapping("/internal/events/{eventId}/decrement-requests")
    void decrementConfirmedRequests(@PathVariable Long eventId);

    @GetMapping("/internal/categories/{catId}")
    CategoryDto getCategory(@PathVariable Long catId);

    @GetMapping("/internal/categories/{catId}/exists")
    Boolean categoryExists(@PathVariable Long catId);

    @GetMapping("/compilations")
    List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                         @RequestParam(defaultValue = "0") int from,
                                         @RequestParam(defaultValue = "10") int size);

    @GetMapping("/compilations/{compId}")
    CompilationDto getCompilation(@PathVariable Long compId);
}