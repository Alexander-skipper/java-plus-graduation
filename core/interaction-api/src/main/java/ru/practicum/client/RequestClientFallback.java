package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        log.warn("Request-service unavailable, returning fallback (0) for eventId: {}", eventId);

        return 0L;
    }

    @Override
    public void incrementConfirmedRequests(Long eventId) {
        log.warn("Request-service unavailable, cannot increment confirmed requests for eventId: {}", eventId);
        // Ничего не делаем, просто логируем
    }

    @Override
    public void decrementConfirmedRequests(Long eventId) {
        log.warn("Request-service unavailable, cannot decrement confirmed requests for eventId: {}", eventId);
        // Ничего не делаем, просто логируем
    }
}
