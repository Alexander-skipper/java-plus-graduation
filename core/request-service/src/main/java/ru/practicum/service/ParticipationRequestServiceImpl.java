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
import ru.practicum.dto.user.UserDto;
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
        log.info("Service trying to create request for user {} and eventId {}", userId, eventId);

        EventResponseDto event = getEventFromClient(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("User " + userId + " tries to create request for his own eventId " + eventId);
        }

        if (requestRepository.findByRequesterIdAndEventId(userId, eventId).isPresent()) {
            throw new ConflictException("Request from user " + userId + " for eventId " + eventId + " already exists");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event " + eventId + " is not published");
        }

        Long eventUserLimit = Long.valueOf(event.getParticipantLimit());
        Long eventUsersRegistered = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        if (eventUserLimit > 0 && eventUsersRegistered >= eventUserLimit) {
            throw new ConflictException("Event " + eventId + " is full");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requesterId(userId)
                .eventId(eventId)
                .created(Timestamp.valueOf(LocalDateTime.now()))
                .status(ParticipationRequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
            incrementConfirmedRequests(eventId);
        }

        log.info("Event {} details: state={}, requestModeration={}, participantLimit={}, confirmedRequests={}",
                eventId, event.getState(), event.getRequestModeration(),
                event.getParticipantLimit(), event.getConfirmedRequests());

        return requestMapper.mapToDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getOtherUsersEventsRequests(Long userId) {
        log.info("Service get requests for user {}", userId);

        if (!userExists(userId)) {
            log.info("User {} does not exist", userId);
            throw new NoSuchElementException("User does not exist");
        }

        return requestRepository.findAllByRequesterId(userId)
                .stream()
                .map(requestMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Request with id " + requestId + " does not exist"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User " + userId + " tries to cancel requests not owned by him");
        }

        if (request.getStatus() == ParticipationRequestStatus.CONFIRMED) {
            decrementConfirmedRequests(request.getEventId());
        }

        request.setStatus(ParticipationRequestStatus.CANCELED);

        return requestMapper.mapToDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getUsersRequestsForUserEvent(Long userId, Long eventId) {
        log.info("Service get requests for user {} and eventId {}", userId, eventId);

        EventResponseDto event = getEventFromClient(eventId);

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ResourceAccessException("Запросы может просматривать только инициатор события");
        }

        return requestRepository.findAllByEventId(eventId)
                .stream()
                .map(requestMapper::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequestStatus) {
        EventResponseDto event = getEventFromClient(eventId);

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ResourceAccessException("Статус запросов может менять только инициатор события");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Moderation is not required or eventId is for unlimited requests");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequestStatus.getRequestIds());

        for (ParticipationRequest request : requests) {
            if (!Objects.equals(request.getEventId(), eventId)) {
                throw new ConflictException("There is no request with id " + request.getId() + " for eventId " + eventId);
            }
            if (request.getStatus() != ParticipationRequestStatus.PENDING) {
                throw new ConflictException("Request with id " + request.getId() + " for eventId " + eventId + " is not in pending state");
            }
        }

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        if (updateRequestStatus.getStatus() == ParticipationRequestStatus.CONFIRMED) {
            Long alreadyConfirmed = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);

            if (alreadyConfirmed >= event.getParticipantLimit()) {
                throw new ConflictException("Event " + eventId + " is full");
            }

            long numberOfFreeSlots = Math.min(requests.size(), event.getParticipantLimit() - alreadyConfirmed);

            for (int i = 0; i < requests.size(); i++) {
                ParticipationRequest request = requests.get(i);
                if (i < numberOfFreeSlots) {
                    request.setStatus(ParticipationRequestStatus.CONFIRMED);
                    confirmedRequests.add(requestMapper.mapToDto(request));
                } else {
                    request.setStatus(ParticipationRequestStatus.REJECTED);
                    rejectedRequests.add(requestMapper.mapToDto(request));
                }
            }
            for (int i = 0; i < numberOfFreeSlots; i++) {
                incrementConfirmedRequests(eventId);
            }

        } else if (updateRequestStatus.getStatus() == ParticipationRequestStatus.REJECTED) {
            requests.forEach(request -> {
                request.setStatus(ParticipationRequestStatus.REJECTED);
                rejectedRequests.add(requestMapper.mapToDto(request));
            });
        } else {
            throw new ConflictException("Status not yet implemented");
        }

        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    private UserDto getUserFromClient(Long userId) {
        try {
            UserDto user = userClient.getUserById(userId);
            if (user == null) {
                throw new NoSuchElementException("User with id " + userId + " not found");
            }
            return user;
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage());
            throw new NoSuchElementException("User with id " + userId + " not found");
        }
    }

    private EventResponseDto getEventFromClient(Long eventId) {
        try {
            EventResponseDto event = eventClient.getEvent(eventId);
            if (event == null) {
                throw new NoSuchElementException("Event with id " + eventId + " not found");
            }
            return event;
        } catch (Exception e) {
            log.error("Error fetching event {}: {}", eventId, e.getMessage());
            throw new NoSuchElementException("Event with id " + eventId + " not found");
        }
    }

    private boolean userExists(Long userId) {
        try {
            return userClient.userExists(userId);
        } catch (Exception e) {
            log.error("Error checking user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    private void incrementConfirmedRequests(Long eventId) {
        try {
            eventClient.incrementConfirmedRequests(eventId);
        } catch (Exception e) {
            log.error("Error incrementing confirmed requests for event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Failed to update event confirmed requests", e);
        }
    }

    private void decrementConfirmedRequests(Long eventId) {
        try {
            eventClient.decrementConfirmedRequests(eventId);
        } catch (Exception e) {
            log.error("Error decrementing confirmed requests for event {}: {}", eventId, e.getMessage());
            throw new RuntimeException("Failed to update event confirmed requests", e);
        }
    }
}