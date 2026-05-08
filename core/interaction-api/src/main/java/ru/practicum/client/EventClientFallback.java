package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.event.EventResponseDto;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class EventClientFallback implements EventClient {

    @Override
    public EventResponseDto getEvent(Long eventId) {
        log.error("Event service is unavailable. Cannot get event by id {}.", eventId);
        throw new RuntimeException("Event service is unavailable");
    }

    @Override
    public Boolean eventExists(Long eventId) {
        log.error("Event service is unavailable. Cannot check event existence.");
        throw new RuntimeException("Event service is unavailable");
    }

    @Override
    public void incrementConfirmedRequests(Long eventId) {
        log.error("Event service is unavailable. Cannot increment confirmed requests for event {}.", eventId);
        throw new RuntimeException("Event service is unavailable");
    }

    @Override
    public void decrementConfirmedRequests(Long eventId) {
        log.error("Event service is unavailable. Cannot decrement confirmed requests for event {}.", eventId);
        throw new RuntimeException("Event service is unavailable");
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        log.error("Event service is unavailable. Cannot get category by id {}.", catId);
        throw new RuntimeException("Event service is unavailable");
    }

    @Override
    public Boolean categoryExists(Long catId) {
        log.error("Event service is unavailable. Cannot check category existence.");
        throw new RuntimeException("Event service is unavailable");
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.warn("Event service is unavailable. Returning empty list for compilations.");
        return Collections.emptyList();
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        log.error("Event service is unavailable. Cannot get compilation by id {}.", compId);
        throw new RuntimeException("Event service is unavailable");
    }
}