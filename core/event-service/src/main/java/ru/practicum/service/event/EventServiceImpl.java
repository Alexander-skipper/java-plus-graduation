package ru.practicum.service.event;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import ru.practicum.client.LocationClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.*;
import ru.practicum.dto.location.ShortLocationResponseDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.error.ConflictException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.stats.client.CollectorGrpcClient;
import ru.practicum.stats.client.RecommendationsGrpcClient;
import ru.practicum.util.EventState;
import ru.practicum.util.EventStateAction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    private final EventMapper mapper;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final LocationClient locationClient;
    private final RequestClient requestClient;
    private final CollectorGrpcClient collectorGrpcClient;
    private final RecommendationsGrpcClient recommendationsGrpcClient;

    private static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventResponseDto create(Long userId, NewEventRequestDto req) {
        log.info("=== CREATE EVENT START ===");
        log.info("create() - userId: {}, request: {}", userId, req);

        UserDto user = getUserFromClient(userId);
        Category category = categoryRepository.findById(req.getCategory())
                .orElseThrow(() -> {
                    log.error("create() - Category with id {} not found!", req.getCategory());
                    return new NoSuchElementException("Category with id " + req.getCategory() + " notFound");
                });

        Event newEvent = mapper.eventRequestToEvent(req, category, user);
        Event savedEvent = eventRepository.save(newEvent);

        EventResponseDto result = mapper.eventToEventResponseDto(savedEvent, getUserShortDto(userId));

        Long confirmedRequests = requestClient.countByEventIdAndStatus(savedEvent.getId(), "CONFIRMED");
        result.setConfirmedRequests(confirmedRequests.intValue());
        return result;
    }

    @Override
    public EventResponseDto getPublicEvent(Long eventId) {
        log.info("getPublicEvent() - looking for eventId={} with state PUBLISHED", eventId);

        Optional<Event> foundEvent = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED);
        if (foundEvent.isEmpty()) {
            log.warn("getPublicEvent() - Event with id {} and state PUBLISHED NOT FOUND", eventId);
            // Доп. проверка: существует ли событие в любом статусе
            boolean exists = eventRepository.existsById(eventId);
            if (exists) {
                Optional<Event> anyStateEvent = eventRepository.findById(eventId);
                log.warn("getPublicEvent() - Event {} exists but with state: {}", eventId,
                        anyStateEvent.map(Event::getState).orElse(null));
            } else {
                log.warn("getPublicEvent() - Event {} does NOT exist in ANY state", eventId);
            }
        }

        Event event = foundEvent
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " notFound"));

        Long confirmedRequests = requestClient.countByEventIdAndStatus(eventId, "CONFIRMED");

        EventResponseDto res = mapper.eventToEventResponseDto(event, getUserShortDto(event.getInitiatorId()));
        res.setConfirmedRequests(confirmedRequests.intValue());

        return res;
    }

    @Override
    public EventResponseDto getUserEvent(Long userId, Long eventId) {
        log.info("getUserEvent() - userId={}, eventId={}", userId, eventId);
        getUserFromClient(userId);
        Event event = findEvent(eventId);
        log.info("getUserEvent() - found event: id={}, state={}, initiatorId={}",
                event.getId(), event.getState(), event.getInitiatorId());
        checkPermission(event, userId);

        Long confirmedRequests = requestClient.countByEventIdAndStatus(eventId, "CONFIRMED");

        EventResponseDto res = mapper.eventToEventResponseDto(event, getUserShortDto(userId));
        res.setConfirmedRequests(confirmedRequests.intValue());
        return res;
    }

    @Override
    public List<ShortEventResponseDto> getUserEvents(Long userId, Pageable pageable) {
        log.info("getUserEvents() - userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
        getUserFromClient(userId);

        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        log.info("getUserEvents() - found {} events for user {}", events.size(), userId);

        for (Event event : events) {
            log.info("getUserEvents() - event: id={}, title={}, state={}, eventDate={}",
                    event.getId(), event.getTitle(), event.getState(),
                    event.getEventDate() != null ? event.getEventDate().format(LOG_FORMATTER) : "null");
        }

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Integer> confirmedRequestsMap = getConfirmedRequestsCounts(eventIds);

        return events.stream()
                .map((event) -> {
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, getUserShortDto(userId));
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .toList();
    }

    @Override
    public List<ShortEventResponseDto> find(EventSearchCriteria criteria) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (criteria.hasCategories()) {
            predicate.and(QEvent.event.category.id.in(criteria.getCategories()));
        }

        if (criteria.hasText()) {
            predicate.and(QEvent.event.annotation.containsIgnoreCase(criteria.getText())
                    .or(QEvent.event.description.containsIgnoreCase(criteria.getText())));
        }

        if (criteria.hasPaid()) {
            predicate.and(QEvent.event.paid.eq(criteria.getPaid()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (!criteria.hasRangeStart() && !criteria.hasRangeEnd()) {
            predicate.and(QEvent.event.eventDate.goe(now));
        } else {
            if (criteria.hasRangeStart()) {
                predicate.and(QEvent.event.eventDate.goe(criteria.getRangeStart()));
            }
            if (criteria.hasRangeEnd()) {
                if (criteria.hasRangeStart() && !criteria.getRangeEnd().isAfter(criteria.getRangeStart())) {
                    throw new IllegalArgumentException("Invalid rangeEnd");
                }
                predicate.and(QEvent.event.eventDate.loe(criteria.getRangeEnd()));
            }
        }

        if (criteria.isOnlyAvailable()) {
            predicate.and(QEvent.event.participantLimit.eq(0));
        }

        predicate.and(QEvent.event.state.eq(EventState.PUBLISHED));

        Pageable pageable = PageRequest.of(criteria.getFrom() / criteria.getSize(),
                criteria.getSize(),
                criteria.getSort());

        Page<Event> events = eventRepository.findAll(predicate, pageable);
        log.info("find() - Найдены события: {}", events.getTotalElements());

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);

        Map<Long, Integer> confirmedRequestsMap = getConfirmedRequestsCounts(eventIds);

        return events.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, userShort);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventResponseDto update(Long userId, Long eventId, UpdateEventRequestDto req) {
        getUserFromClient(userId);
        Event event = findEvent(eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot update published eventId");
        }

        if (event.getEventDate().minusHours(2L).isBefore(LocalDateTime.now())) {
            throw new ConflictException("Event could be changed only 2 hours before now");
        }

        checkPermission(event, userId);
        Category category = null;

        if (req.getCategory() != null) {
            category = categoryRepository.findById(req.getCategory())
                    .orElseThrow(() -> new NoSuchElementException("Category with id " + req.getCategory() + " notFound"));
        }

        Event updatingEvent = mapper.updateEventField(event, req, category);

        if (req.getStateAction() == EventStateAction.SEND_TO_REVIEW) {
            if (updatingEvent.getState() == EventState.CANCELED || updatingEvent.getState() == EventState.PENDING) {
                updatingEvent.setState(EventState.PENDING);
            }
        }

        if (req.getStateAction() == EventStateAction.CANCEL_REVIEW) {
            updatingEvent.setState(EventState.CANCELED);
        }

        EventResponseDto result = mapper.eventToEventResponseDto(updatingEvent, getUserShortDto(userId));
        Long confirmedRequests = requestClient.countByEventIdAndStatus(eventId, "CONFIRMED");
        result.setConfirmedRequests(confirmedRequests.intValue());

        return result;
    }

    @Override
    public List<AdminEventResponseDto> findAdminEvents(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable) {

        log.info("findAdminEvents() - users={}, states={}, categories={},",
                users, states, categories);

        List<Event> events = eventRepository.findAdminEvents(
                users, states, categories, rangeStart, rangeEnd, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);
        Map<Long, Integer> confirmedRequestsMap = getConfirmedRequestsCounts(eventIds);

        return events.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    AdminEventResponseDto dto = mapper.toAdminEventFullDto(event, userShort);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventResponseDto getEventById(Long eventId) {
        log.info("=== getEventById() START ===");
        log.info("getEventById() - looking for eventId={}", eventId);
        Event event = findEvent(eventId);

        Long confirmedRequests = requestClient.countByEventIdAndStatus(eventId, "CONFIRMED");

        EventResponseDto dto = mapper.eventToEventResponseDto(event, getUserShortDto(event.getInitiatorId()));
        dto.setConfirmedRequests(confirmedRequests.intValue());
        return dto;
    }

    @Override
    public EventResponseDto getInternalEventById(Long eventId) {
        log.info("getInternalEventById() - looking for eventId={}", eventId);
        Event event = findEvent(eventId);

        Long confirmedRequests = requestClient.countByEventIdAndStatus(eventId, "CONFIRMED");

        EventResponseDto dto = mapper.eventToEventResponseDto(event, getUserShortDto(event.getInitiatorId()));
        dto.setConfirmedRequests(confirmedRequests.intValue());

        return dto;
    }

    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public Map<Long, Integer> getConfirmedRequestsCounts(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        try {
            Map<Long, Long> counts = requestClient.countByEventIdsAndStatus(eventIds, "CONFIRMED");
            return counts.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().intValue()));
        } catch (Exception e) {
            log.error("Error getting confirmed requests counts: {}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public List<ShortEventResponseDto> findEventsByLocation(Long locationId, Pageable pageable) {
        log.info("Finding events for location id: {}", locationId);

        ShortLocationResponseDto location = locationClient.getLocation(locationId);
        if (location == null) {
            throw new NoSuchElementException("Location with id " + locationId + " not found");
        }

        if (location.getLatitude() == null || location.getLongitude() == null) {
            throw new IllegalArgumentException("Location " + locationId + " has invalid coordinates");
        }

        List<Event> events = eventRepository.findEventsWithinLocationRadius(
                location.getLatitude(),
                location.getLongitude(),
                location.getRadius() != null ? location.getRadius() : 1.0,
                pageable
        );

        log.info("Found {} events for location id: {} (center: lat={}, lon={}, radius={}km)",
                events.size(), locationId, location.getLatitude(), location.getLongitude(),
                location.getRadius() != null ? location.getRadius() : 1.0);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);
        Map<Long, Integer> confirmedRequestsMap = getConfirmedRequestsCounts(eventIds);

        return events.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, userShort);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ShortEventResponseDto> findEventsNear(Double lat, Double lon, Double radius, Pageable pageable) {
        log.info("Finding events for user at coordinates: lat={}, lon={}, radius={}km", lat, lon, radius);

        validateCoordinatesAndRadius(lat, lon, radius);

        List<ShortLocationResponseDto> userLocations = locationClient.findLocationsNear(lat, lon, radius);
        if (userLocations.isEmpty()) {
            log.info("User at coordinates lat={}, lon={} doesn't get at any location", lat, lon);
            return Collections.emptyList();
        }
        log.info("User is in {} locations", userLocations.size());

        Set<Event> allEvents = new HashSet<>();

        for (ShortLocationResponseDto location : userLocations) {
            List<Event> eventsInLocation = eventRepository.findEventsWithinLocationRadius(
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getRadius() != null ? location.getRadius() : 1.0,
                    pageable
            );
            allEvents.addAll(eventsInLocation);
            log.info("Found {} events in location id: {}", eventsInLocation.size(), location.getId());
        }

        if (allEvents.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = allEvents.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        List<Long> eventIds = allEvents.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);
        Map<Long, Integer> confirmedRequestsMap = getConfirmedRequestsCounts(eventIds);

        return allEvents.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, userShort);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminEventResponseDto updateAdminEvent(Long eventId, UpdateEventAdminRequest req) {
        log.info("=== UPDATE ADMIN EVENT START ===");
        log.info("updateAdminEvent() - eventId={}, stateAction={}", eventId, req.getStateAction());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("updateAdminEvent() - Event with id {} NOT FOUND in DB!", eventId);
                    return new NoSuchElementException("Event with id " + eventId + " not found");
                });

        log.info("updateAdminEvent() - found event: id={}, title={}, currentState={}, eventDate={}",
                event.getId(), event.getTitle(), event.getState(),
                event.getEventDate() != null ? event.getEventDate().format(LOG_FORMATTER) : "null");

        Event updatedEvent = updateEventByAdmin(event, req);
        log.info("updateAdminEvent() - updated event: id={}, newState={}", updatedEvent.getId(), updatedEvent.getState());

        AdminEventResponseDto dto = mapper.toAdminEventFullDto(updatedEvent, getUserShortDto(event.getInitiatorId()));
        Long confirmedRequests = requestClient.countByEventIdAndStatus(eventId, "CONFIRMED");
        dto.setConfirmedRequests(confirmedRequests.intValue());

        return dto;
    }

    @Transactional
    public Event updateEventByAdmin(Event event, UpdateEventAdminRequest update) {
        log.info("updateEventByAdmin() - START. Event id={}, currentState={}, requested action={}",
                event.getId(), event.getState(), update.getStateAction());

        if (update.getCategory() != null) {
            log.info("updateEventByAdmin() - updating category to id={}", update.getCategory());
            Category category = categoryRepository.findById(update.getCategory())
                    .orElseThrow(() -> new NoSuchElementException("Category with id " + update.getCategory() + " doesnt exist "));
            event.setCategory(category);
        }

        EventState state = event.getState();
        EventStateAction updateStateAction = update.getStateAction();

        if (updateStateAction == EventStateAction.PUBLISH_EVENT) {
            log.info("updateEventByAdmin() - Attempting to PUBLISH event {}", event.getId());

            if (state != EventState.PENDING) {
                log.error("updateEventByAdmin() - Cannot publish: event state is {}, expected PENDING", state);
                throw new ConflictException("Only events with waiting status could be published");
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourBefore = event.getEventDate().minusHours(1L);
            boolean canPublish = oneHourBefore.isAfter(now);
            log.info("updateEventByAdmin() - eventDate={}, now={}, oneHourBefore={}, canPublish={}",
                    event.getEventDate().format(LOG_FORMATTER),
                    now.format(LOG_FORMATTER),
                    oneHourBefore.format(LOG_FORMATTER),
                    canPublish);

            if (!canPublish) {
                log.error("updateEventByAdmin() - Cannot publish: event date is too close! EventDate={}, now={}",
                        event.getEventDate().format(LOG_FORMATTER), now.format(LOG_FORMATTER));
                throw new ConflictException("Event could be changed only one hour before now");
            }

            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
            log.info("updateEventByAdmin() - Event {} successfully PUBLISHED at {}",
                    event.getId(), event.getPublishedOn().format(LOG_FORMATTER));

        } else if (updateStateAction == EventStateAction.REJECT_EVENT) {
            log.info("updateEventByAdmin() - Attempting to REJECT event {}", event.getId());
            if (state == EventState.PUBLISHED) {
                log.error("updateEventByAdmin() - Cannot reject: event is already PUBLISHED");
                throw new ConflictException("Published eventId could not be rejected");
            }
            event.setState(EventState.CANCELED);
            log.info("updateEventByAdmin() - Event {} REJECTED (state=CANCELED)", event.getId());
        } else {
            log.info("updateEventByAdmin() - No state action requested (stateAction={})", updateStateAction);
        }

        if (update.getTitle() != null) {
            log.info("updateEventByAdmin() - updating title from '{}' to '{}'", event.getTitle(), update.getTitle());
            event.setTitle(update.getTitle());
        }

        if (update.getAnnotation() != null) {
            log.info("updateEventByAdmin() - updating annotation (length from {} to {})",
                    event.getAnnotation() != null ? event.getAnnotation().length() : 0,
                    update.getAnnotation().length());
            event.setAnnotation(update.getAnnotation());
        }

        if (update.getDescription() != null) {
            log.info("updateEventByAdmin() - updating description (length from {} to {})",
                    event.getDescription() != null ? event.getDescription().length() : 0,
                    update.getDescription().length());
            event.setDescription(update.getDescription());
        }

        if (update.getEventDate() != null) {
            log.info("updateEventByAdmin() - updating eventDate from {} to {}",
                    event.getEventDate().format(LOG_FORMATTER),
                    update.getEventDate().format(LOG_FORMATTER));
            if (update.getEventDate().isBefore(LocalDateTime.now())) {
                log.error("updateEventByAdmin() - event date cannot be in the past: {}", update.getEventDate().format(LOG_FORMATTER));
                throw new ConflictException("Event date couldnt be in the past");
            }
            event.setEventDate(update.getEventDate());
        }

        if (update.getParticipantLimit() != null) {
            log.info("updateEventByAdmin() - updating participantLimit from {} to {}", event.getParticipantLimit(), update.getParticipantLimit());
            if (update.getParticipantLimit() < 0) {
                throw new IllegalArgumentException("Participant limit cannot be negative");
            }
            event.setParticipantLimit(update.getParticipantLimit());
        }

        if (update.getLocation() != null) {
            log.info("updateEventByAdmin() - updating location from (lat={}, lon={}) to (lat={}, lon={})",
                    event.getLat(), event.getLon(), update.getLocation().getLat(), update.getLocation().getLon());
            event.setLat(update.getLocation().getLat());
            event.setLon(update.getLocation().getLon());
        }

        if (update.getPaid() != null) {
            log.info("updateEventByAdmin() - updating paid from {} to {}", event.getPaid(), update.getPaid());
            event.setPaid(update.getPaid());
        }

        if (update.getRequestModeration() != null) {
            log.info("updateEventByAdmin() - updating requestModeration from {} to {}", event.getRequestModeration(), update.getRequestModeration());
            event.setRequestModeration(update.getRequestModeration());
        }

        log.info("updateEventByAdmin() - FINISHED. Event id={}, final state={}", event.getId(), event.getState());
        return event;
    }

    @Override
    public List<ShortEventResponseDto> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Getting recommendations for user: {}, maxResults={}", userId, maxResults);

        getUserFromClient(userId);

        List<RecommendationsGrpcClient.RecommendedEvent> recommendations =
                recommendationsGrpcClient.getRecommendationsForUser(userId, maxResults);

        if (recommendations.isEmpty()) {
            log.info("No recommendations for user {}", userId);
            return Collections.emptyList();
        }

        List<Long> eventIds = recommendations.stream()
                .map(RecommendationsGrpcClient.RecommendedEvent::eventId)
                .collect(Collectors.toList());

        List<Event> events = eventRepository.findAllById(eventIds);

        Map<Long, Double> scoreMap = recommendations.stream()
                .collect(Collectors.toMap(
                        RecommendationsGrpcClient.RecommendedEvent::eventId,
                        RecommendationsGrpcClient.RecommendedEvent::score
                ));

        events.sort((e1, e2) -> {
            Double s1 = scoreMap.getOrDefault(e1.getId(), 0.0);
            Double s2 = scoreMap.getOrDefault(e2.getId(), 0.0);
            return s2.compareTo(s1);
        });

        if (events.size() > maxResults) {
            events = events.subList(0, maxResults);
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        List<Long> resultEventIds = events.stream().map(Event::getId).collect(Collectors.toList());

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);
        Map<Long, Integer> confirmedRequestsMap = getConfirmedRequestsCounts(resultEventIds);

        return events.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, userShort);
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(event.getId(), 0));
                    if (event.getRating() != null) {
                        dto.setRating(event.getRating());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        log.info("User {} likes event {}", userId, eventId);

        getUserFromClient(userId);
        Event event = findEvent(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot like unpublished event");
        }

        Long confirmedRequests = requestClient.countByEventIdAndStatus(eventId, "CONFIRMED");
        if (confirmedRequests == 0) {
            throw new ConflictException("You can only like events you are registered for");
        }

        collectorGrpcClient.sendLike(userId, eventId);
        log.info("Sent LIKE action for userId={}, eventId={}", userId, eventId);

        updateEventRating(eventId);
    }

    @Override
    @Transactional
    public void updateEventRating(Long eventId) {
        log.info("Updating rating for event: {}", eventId);

        Double rating = fetchEventRating(eventId);

        Event event = findEvent(eventId);
        event.setRating(rating);
        eventRepository.save(event);

        log.info("Updated rating for event {} to {}", eventId, rating);
    }

    private Double fetchEventRating(Long eventId) {
        try {
            List<RecommendationsGrpcClient.RecommendedEvent> result =
                    recommendationsGrpcClient.getInteractionsCount(List.of(eventId));
            return result.stream()
                    .findFirst()
                    .map(RecommendationsGrpcClient.RecommendedEvent::score)
                    .orElse(0.0);
        } catch (Exception e) {
            log.error("Failed to fetch rating for event {}: {}", eventId, e.getMessage());
            return 0.0;
        }
    }



    private UserDto getUserFromClient(Long userId) {
        log.info("getUserFromClient() - fetching user {}", userId);
        try {
            UserDto user = userClient.getUserById(userId);
            if (user == null) {
                log.error("getUserFromClient() - user {} is null", userId);
                throw new NoSuchElementException("User with id " + userId + " not found");
            }
            log.info("getUserFromClient() - user found: id={}, name={}", user.getId(), user.getName());
            return user;
        } catch (Exception e) {
            log.error("Error fetching user {}: {}", userId, e.getMessage(), e);
            throw new NoSuchElementException("User with id " + userId + " not found");
        }
    }

    private UserShortDto getUserShortDto(Long userId) {
        UserDto userDto = getUserFromClient(userId);
        return UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();
    }

    private Map<Long, UserShortDto> getUsersShortDto(Set<Long> userIds) {
        log.info("getUsersShortDto() - fetching {} users", userIds.size());
        return userIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::getUserShortDto
                ));
    }

    private Event findEvent(Long eventId) {
        log.info("findEvent() - looking for event by id={}", eventId);
        Optional<Event> optional = eventRepository.findById(eventId);
        if (optional.isEmpty()) {
            log.error("findEvent() - Event with id {} NOT FOUND in database!", eventId);
            // Проверяем все события в БД для отладки
            List<Event> allEvents = eventRepository.findAll();
            log.info("findEvent() - Total events in DB: {}", allEvents.size());
            for (Event e : allEvents) {
                log.info("findEvent() - existing event: id={}, title={}", e.getId(), e.getTitle());
            }
            throw new NoSuchElementException("Event with id " + eventId + " notFound");
        }
        Event event = optional.get();
        log.info("findEvent() - found event: id={}, title={}, state={}", event.getId(), event.getTitle(), event.getState());
        return event;
    }

    private void checkPermission(Event event, Long userId) {
        log.info("checkPermission() - event initiatorId={}, requesting userId={}", event.getInitiatorId(), userId);
        if (!event.getInitiatorId().equals(userId)) {
            log.error("checkPermission() - ACCESS DENIED! User {} cannot access event of user {}", userId, event.getInitiatorId());
            throw new ResourceAccessException("Access to eventId " + event.getId() + " forbidden");
        }
        log.info("checkPermission() - access granted");
    }

    private void validateCoordinatesAndRadius(Double lat, Double lon, Double radius) {
        if (lat == null || lon == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
        if (radius == null || radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
    }

}