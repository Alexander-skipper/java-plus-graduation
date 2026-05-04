package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.compilation.CompilationDto;

import java.util.Collections;

@Slf4j
@Component
public class CompilationClientFallback implements CompilationClient {

    @Override
    public CompilationDto getCompilation(Long compId) {
        log.warn("Compilation-service unavailable, returning fallback for compId: {}", compId);

        CompilationDto fallback = new CompilationDto();
        fallback.setId(compId);
        fallback.setTitle("Unknown Compilation");
        fallback.setPinned(false);
        fallback.setEvents(Collections.emptySet());
        return fallback;
    }

    @Override
    public Boolean isEventInCompilation(Long eventId) {
        log.warn("Compilation-service unavailable, returning false for eventId: {}", eventId);

        return false;
    }
}
