package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;

@FeignClient(
        name = "compilation-service",
        fallback = CompilationClientFallback.class
)
public interface CompilationClient {

    @GetMapping("/internal/compilations/{compId}")
    CompilationDto getCompilation(@PathVariable("compId") Long compId);

    @GetMapping("/internal/compilations/eventId/{eventId}")
    Boolean isEventInCompilation(@PathVariable("eventId") Long eventId);
}
