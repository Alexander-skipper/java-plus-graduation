package ru.practicum.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.event.EventResponseDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.error.ConflictException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.util.ParticipationRequestStatus;
import ru.practicum.util.EventState;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper requestMapper;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("=== CREATE REQUEST START ===");
        log.info("createRequest() - userId={}, eventId={}", userId, eventId);

        // Проверяем существование пользователя
        log.info("createRequest() - checking if user {} exists", userId);
        boolean userExists = userExists(userId);
        log.info("createRequest() - userExists={}", userExists);

        if (!userExists) {
            log.error("createRequest() - User {} does not exist!", userId);
            throw new NoSuchElementException("User with id " + userId + " does not exist");
        }

        // Пытаемся получить событие через Feign клиент
        log.info("createRequest() - calling eventClient.getEvent({})", eventId);
        EventResponseDto event = null;
        try {
            event = getEventFromClient(eventId);
            log.info("createRequest() - eventClient.getEvent returned: {}", event);
            if (event != null) {
                log.info("createRequest() - event details: id={}, title={}, state={}, initiatorId={}, participantLimit={}, requestModeration={}",
                        event.getId(), event.getTitle(), event.getState(),
                        event.getInitiator() != null ? event.getInitiator().getId() : "null",
                        event.getParticipantLimit(), event.getRequestModeration());
            } else {
                log.error("createRequest() - eventClient.getEvent returned NULL for eventId={}", eventId);
            }
        } catch (Exception e) {
            log.error("createRequest() - EXCEPTION calling eventClient.getEvent({}): {}", eventId, e.getMessage(), e);
            throw new NoSuchElementException("Event with id " + eventId + " not found (Feign call failed)");
        }

        if (event == null) {
            log.error("createRequest() - Event with id {} not found (event is null)", eventId);
            throw new NoSuchElementException("Event with id " + eventId + " not found");
        }

        if (event.getInitiator() == null) {
            log.error("createRequest() - Event {} has null initiator!", eventId);
            throw new IllegalStateException("Event " + eventId + " has no initiator");
        }

        if (event.getInitiator().getId().equals(userId)) {
            log.error("createRequest() - User {} is the initiator of event {}, cannot request own event", userId, eventId);
            throw new ConflictException("User " + userId + " tries to create request for his own eventId " + eventId);
        }

        if (requestRepository.findByRequesterIdAndEventId(userId, eventId).isPresent()) {
            log.error("createRequest() - Request from user {} for event {} already exists", userId, eventId);
            throw new ConflictException("Request from user " + userId + " for eventId " + eventId + " already exists");
        }

        // Проверяем, что статус события соответствует ожидаемому
        log.info("createRequest() - checking event state: expected PUBLISHED, actual={}", event.getState());
        if (event.getState() != EventState.PUBLISHED) {
            log.error("createRequest() - Event {} is not published (state={})", eventId, event.getState());
            throw new ConflictException("Event " + eventId + " is not published");
        }

        Long eventUserLimit = Long.valueOf(event.getParticipantLimit());
        Long eventUsersRegistered = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        log.info("createRequest() - eventUserLimit={}, eventUsersRegistered={}", eventUserLimit, eventUsersRegistered);

        if (eventUserLimit > 0 && eventUsersRegistered >= eventUserLimit) {
            log.error("createRequest() - Event {} is full (limit={}, registered={})", eventId, eventUserLimit, eventUsersRegistered);
            throw new ConflictException("Event " + eventId + " is full");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requesterId(userId)
                .eventId(eventId)
                .created(Timestamp.valueOf(LocalDateTime.now()))
                .status(ParticipationRequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            log.info("createRequest() - Auto-confirming request because requestModeration={}, participantLimit={}",
                    event.getRequestModeration(), event.getParticipantLimit());
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
        }

        log.info("createRequest() - saving request with status={}", request.getStatus());
        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("=== CREATE REQUEST SUCCESS: requestId={}, status={} ===", savedRequest.getId(), savedRequest.getStatus());

        return requestMapper.mapToDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getOtherUsersEventsRequests(Long userId) {
        log.info("=== GET OTHER USERS REQUESTS START ===");
        log.info("getOtherUsersEventsRequests() - userId={}", userId);

        if (!userExists(userId)) {
            log.error("getOtherUsersEventsRequests() - User {} does not exist!", userId);
            throw new NoSuchElementException("User does not exist");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);
        log.info("getOtherUsersEventsRequests() - found {} requests for user {}", requests.size(), userId);

        for (ParticipationRequest req : requests) {
            log.info("getOtherUsersEventsRequests() - request: id={}, eventId={}, status={}",
                    req.getId(), req.getEventId(), req.getStatus());
        }

        return requests.stream()
                .map(requestMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("=== CANCEL REQUEST START ===");
        log.info("cancelRequest() - userId={}, requestId={}", userId, requestId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("cancelRequest() - Request with id {} does not exist!", requestId);
                    return new NoSuchElementException("Request with id " + requestId + " does not exist");
                });

        log.info("cancelRequest() - found request: eventId={}, requesterId={}, status={}",
                request.getEventId(), request.getRequesterId(), request.getStatus());

        if (!request.getRequesterId().equals(userId)) {
            log.error("cancelRequest() - User {} tries to cancel request belonging to user {}", userId, request.getRequesterId());
            throw new ConflictException("User " + userId + " tries to cancel requests not owned by him");
        }

        if (request.getStatus() == ParticipationRequestStatus.CONFIRMED) {
            log.info("cancelRequest() - Request was CONFIRMED, request for event {}. The event service will" +
                    " query the actual count when needed.", request.getEventId());
        }

        request.setStatus(ParticipationRequestStatus.CANCELED);
        log.info("cancelRequest() - Request status changed to CANCELED");

        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("=== CANCEL REQUEST SUCCESS: requestId={}, newStatus={} ===", savedRequest.getId(), savedRequest.getStatus());

        return requestMapper.mapToDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUsersRequestsForUserEvent(Long userId, Long eventId) {
        log.info("=== GET USERS REQUESTS FOR EVENT START ===");
        log.info("getUsersRequestsForUserEvent() - userId={}, eventId={}", userId, eventId);

        EventResponseDto event = getEventFromClient(eventId);
        log.info("getUsersRequestsForUserEvent() - event initiatorId={}, requesting userId={}",
                event.getInitiator().getId(), userId);

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            log.error("getUsersRequestsForUserEvent() - User {} is not initiator of event {}, access denied", userId, eventId);
            throw new ResourceAccessException("Запросы может просматривать только инициатор события");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        log.info("getUsersRequestsForUserEvent() - found {} requests for event {}", requests.size(), eventId);

        for (ParticipationRequest req : requests) {
            log.info("getUsersRequestsForUserEvent() - request: id={}, requesterId={}, status={}",
                    req.getId(), req.getRequesterId(), req.getStatus());
        }

        return requests.stream()
                .map(requestMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequestStatus) {
        log.info("=== UPDATE REQUEST STATUS START ===");
        log.info("updateRequestStatus() - userId={}, eventId={}, status={}, requestIds={}",
                userId, eventId, updateRequestStatus.getStatus(), updateRequestStatus.getRequestIds());

        EventResponseDto event = getEventFromClient(eventId);
        log.info("updateRequestStatus() - event initiatorId={}, requesting userId={}",
                event.getInitiator().getId(), userId);

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            log.error("updateRequestStatus() - User {} is not initiator of event {}", userId, eventId);
            throw new ResourceAccessException("Статус запросов может менять только инициатор события");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            log.error("updateRequestStatus() - Moderation is not required (requestModeration={}, participantLimit={})",
                    event.getRequestModeration(), event.getParticipantLimit());
            throw new ConflictException("Moderation is not required or eventId is for unlimited requests");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequestStatus.getRequestIds());
        log.info("updateRequestStatus() - found {} requests by IDs", requests.size());

        for (ParticipationRequest request : requests) {
            log.info("updateRequestStatus() - validating request: id={}, eventId={}, status={}",
                    request.getId(), request.getEventId(), request.getStatus());

            if (!Objects.equals(request.getEventId(), eventId)) {
                log.error("updateRequestStatus() - Request {} belongs to event {}, not to event {}",
                        request.getId(), request.getEventId(), eventId);
                throw new ConflictException("There is no request with id " + request.getId() + " for eventId " + eventId);
            }
            if (request.getStatus() != ParticipationRequestStatus.PENDING) {
                log.error("updateRequestStatus() - Request {} is not in PENDING state (current={})",
                        request.getId(), request.getStatus());
                throw new ConflictException("Request with id " + request.getId() + " for eventId " + eventId + " is not in pending state");
            }
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        if (updateRequestStatus.getStatus() == ParticipationRequestStatus.CONFIRMED) {
            Long alreadyConfirmed = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
            log.info("updateRequestStatus() - CONFIRMED action: alreadyConfirmed={}, participantLimit={}",
                    alreadyConfirmed, event.getParticipantLimit());

            if (alreadyConfirmed >= event.getParticipantLimit()) {
                log.error("updateRequestStatus() - Event {} is full (alreadyConfirmed={}, limit={})",
                        eventId, alreadyConfirmed, event.getParticipantLimit());
                throw new ConflictException("Event " + eventId + " is full");
            }

            long numberOfFreeSlots = Math.min(requests.size(), event.getParticipantLimit() - alreadyConfirmed);
            log.info("updateRequestStatus() - numberOfFreeSlots={}", numberOfFreeSlots);

            for (int i = 0; i < requests.size(); i++) {
                ParticipationRequest request = requests.get(i);
                if (i < numberOfFreeSlots) {
                    request.setStatus(ParticipationRequestStatus.CONFIRMED);
                    confirmedRequests.add(requestMapper.mapToDto(request));
                    log.info("updateRequestStatus() - Request {} CONFIRMED", request.getId());
                } else {
                    request.setStatus(ParticipationRequestStatus.REJECTED);
                    rejectedRequests.add(requestMapper.mapToDto(request));
                    log.info("updateRequestStatus() - Request {} REJECTED (no free slots)", request.getId());
                }
            }

        } else if (updateRequestStatus.getStatus() == ParticipationRequestStatus.REJECTED) {
            log.info("updateRequestStatus() - REJECTED action: rejecting all {} requests", requests.size());
            requests.forEach(request -> {
                request.setStatus(ParticipationRequestStatus.REJECTED);
                rejectedRequests.add(requestMapper.mapToDto(request));
                log.info("updateRequestStatus() - Request {} REJECTED", request.getId());
            });
        } else {
            log.error("updateRequestStatus() - Unsupported status: {}", updateRequestStatus.getStatus());
            throw new ConflictException("Status not yet implemented");
        }

        requestRepository.saveAll(requests);
        log.info("updateRequestStatus() - saved {} requests", requests.size());
        log.info("=== UPDATE REQUEST STATUS SUCCESS: confirmed={}, rejected={} ===",
                confirmedRequests.size(), rejectedRequests.size());

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    private EventResponseDto getEventFromClient(Long eventId) {
        log.info("getEventFromClient() - ENTER. eventId={}", eventId);
        try {
            log.info("getEventFromClient() - calling eventClient.getEvent({})", eventId);
            EventResponseDto event = eventClient.getEvent(eventId);
            log.info("getEventFromClient() - eventClient.getEvent returned: {}", event);

            if (event == null) {
                log.error("getEventFromClient() - eventClient.getEvent returned NULL for eventId={}", eventId);
                throw new NoSuchElementException("Event with id " + eventId + " not found");
            }

            log.info("getEventFromClient() - SUCCESS. Event id={}, title={}, state={}",
                    event.getId(), event.getTitle(), event.getState());
            return event;
        } catch (feign.FeignException e) {
            log.error("getEventFromClient() - FEIGN EXCEPTION: status={}, message={}", e.status(), e.getMessage(), e);
            log.error("getEventFromClient() - Response body: {}", e.contentUTF8());
            throw new NoSuchElementException("Event with id " + eventId + " not found (Feign error: " + e.status() + ")");
        } catch (Exception e) {
            log.error("getEventFromClient() - GENERAL EXCEPTION: {}", e.getMessage(), e);
            throw new NoSuchElementException("Event with id " + eventId + " not found");
        }
    }

    private boolean userExists(Long userId) {
        log.info("userExists() - checking user {}", userId);
        try {
            boolean exists = userClient.userExists(userId);
            log.info("userExists() - user {} exists={}", userId, exists);
            return exists;
        } catch (Exception e) {
            log.error("userExists() - Error checking user {}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

}