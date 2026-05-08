package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class RequestClientFallback implements RequestClient {

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.error("Request service is unavailable. Cannot create request.");
        throw new RuntimeException("Request service is unavailable");
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId) {
        log.warn("Request service is unavailable. Returning empty list.");
        return Collections.emptyList();
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.error("Request service is unavailable. Cannot cancel request.");
        throw new RuntimeException("Request service is unavailable");
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.warn("Request service is unavailable. Returning empty list.");
        return Collections.emptyList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        log.error("Request service is unavailable. Cannot update request status.");
        throw new RuntimeException("Request service is unavailable");
    }

    @Override
    public Long countByEventIdAndStatus(Long eventId, String status) {
        log.warn("Request service is unavailable. Returning 0 for count.");
        return 0L;
    }
}
