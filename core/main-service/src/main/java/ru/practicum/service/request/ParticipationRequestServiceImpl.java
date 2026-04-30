package ru.practicum.service.request;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.error.ConflictException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.Event;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.model.User;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.util.EventState;
import ru.practicum.util.ParticipationRequestStatus;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@AllArgsConstructor
@Slf4j
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestMapper requestMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Service trying to create request for user {} and event {}", userId, eventId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    return new NoSuchElementException("User with id " + userId + "does not exist");
                });

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    return new NoSuchElementException("Event with id " + eventId + "does not exist");
                });

        if (event.getInitiator().getId().equals(user.getId())) {
            throw new ConflictException("User " + userId + " tries to create request for his own event " + eventId);
        }

        if (requestRepository.findByRequesterIdAndEventId(userId, eventId).isPresent()) {
            throw new ConflictException("Request from user " + userId + " for event " + eventId + " already exists");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event " + eventId + " is not published");
        }

        Long eventUserLimit = Long.valueOf(event.getParticipantLimit());
        Long eventUsersRegistered = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED);
        if (eventUserLimit > 0 && eventUsersRegistered >= eventUserLimit) {
            throw new ConflictException("Event " + eventId + " is full");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requester(user)
                .event(event)
                .created(Timestamp.valueOf(LocalDateTime.now()))
                .status(ParticipationRequestStatus.PENDING)
                .build();

        if (event.getRequestModeration() == false || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }
        log.info("Event {} details: state={}, requestModeration={}, participantLimit={}, confirmedRequests={}",
                eventId, event.getState(), event.getRequestModeration(),
                event.getParticipantLimit(), event.getConfirmedRequests());
        return requestMapper.mapToDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getOtherUsersEventsRequests(Long userId) {
        log.info("Service get requests for user {}", userId);
        if (!userRepository.existsById(userId)) {
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

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("User " + userId + " tries to cancel requests not owned bu him");
        }

        if (request.getStatus() == ParticipationRequestStatus.CONFIRMED) {
            Event event = eventRepository.findById(request.getEvent().getId())
                    .orElseThrow(() -> new NoSuchElementException("Event with id " + request.getEvent().getId() + " does not exist"));

            event.setConfirmedRequests(event.getConfirmedRequests() - 1);
            eventRepository.save(event);
        }

        request.setStatus(ParticipationRequestStatus.CANCELED);

        return requestMapper.mapToDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getUsersRequestsForUserEvent(Long userId, Long eventId) {
        log.info("Service get requests for user {} and event {}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " does not exist"));

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
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest updateRequestStatus) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " does not exist"));
        if (!(Objects.equals(event.getInitiator().getId(), userId))) {
            throw new ResourceAccessException("Статус запросов может менять только инициатор события");
        }

        if (event.getRequestModeration() == false || event.getParticipantLimit() == 0) {
            throw new ConflictException("Moderation is not required or event is for unlimited requests");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(updateRequestStatus.getRequestIds());
        for (ParticipationRequest request : requests) {
            if (!Objects.equals(request.getEvent().getId(), eventId)) {
                throw new ConflictException("There is no request with id " + request.getId() + " for event " + eventId);
            }
            if (request.getStatus() != ParticipationRequestStatus.PENDING) {
                throw new ConflictException("Request with id " + request.getId() + " for event " + eventId + "is not in pending state");
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
            event.setConfirmedRequests((int)(alreadyConfirmed + numberOfFreeSlots));

        } else if (updateRequestStatus.getStatus() == ParticipationRequestStatus.REJECTED) {
            requests.forEach(request -> {
                request.setStatus(ParticipationRequestStatus.REJECTED);
                rejectedRequests.add(requestMapper.mapToDto(request));
            });
        } else {
            throw new ConflictException("Status not yet implemented");
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

}
