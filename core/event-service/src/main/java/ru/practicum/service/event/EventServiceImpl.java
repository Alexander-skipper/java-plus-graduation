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
import ru.practicum.client.UserClient;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.location.ShortLocationResponseDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.error.ConflictException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.util.EventState;
import ru.practicum.util.EventStateAction;
import ru.practicum.stats.client.StatsClient;

import java.time.LocalDateTime;
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
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventResponseDto create(Long userId, NewEventRequestDto req) {
        UserDto user = getUserFromClient(userId);

        Category category = categoryRepository.findById(req.getCategory())
                .orElseThrow(() -> new NoSuchElementException("Category with id " + req.getCategory() + " notFound"));

        Event newEvent = mapper.eventRequestToEvent(req, category, user);

        Event savedEvent = eventRepository.save(newEvent);
        log.info("Создано новое событие {} от пользователя {}", savedEvent, user);

        return mapper.eventToEventResponseDto(savedEvent, getUserShortDto(userId));
    }

    @Override
    public EventResponseDto getPublicEvent(Long eventId) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " notFound"));
        log.info("Найдено событие {}", event);

        Long views = getViews(eventId);
        EventResponseDto res = mapper.eventToEventResponseDto(event, getUserShortDto(event.getInitiatorId()));
        res.setViews(views);

        return res;
    }

    @Override
    public EventResponseDto getUserEvent(Long userId, Long eventId) {
        getUserFromClient(userId);
        Event event = findEvent(eventId);

        log.info("Найдено событие {}", event);
        checkPermission(event, userId);

        return mapper.eventToEventResponseDto(event, getUserShortDto(userId));
    }

    @Override
    public List<ShortEventResponseDto> getUserEvents(Long userId, Pageable pageable) {
        getUserFromClient(userId);

        return eventRepository.findAllByInitiatorId(userId, pageable)
                .stream()
                .map((event) -> mapper.eventToShortEventResponseDto(event, getUserShortDto(userId)))
                .toList();
    }

    @Override
    public List<ShortEventResponseDto> find(EventSearchCriteria criteria) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (criteria.hasCategories()) {
            predicate.and(QEvent.event.category.id.in(criteria.getCategories()));
        }

        if (criteria.hasText()) {
            predicate.and(QEvent.event.annotation.contains(criteria.getText())
                    .or(QEvent.event.description.contains(criteria.getText())));
        }

        if (criteria.hasPaid()) {
            predicate.and(QEvent.event.paid.eq(criteria.getPaid()));
        }

        if (criteria.hasRangeStart()) {
            predicate.and(QEvent.event.eventDate.goe(criteria.getRangeStart()));
        }

        if (criteria.hasRangeEnd()) {
            if (criteria.hasRangeStart() && !criteria.getRangeEnd().isAfter(criteria.getRangeStart())) {
                throw new IllegalArgumentException("Invalid rangeEnd");
            }
            predicate.and(QEvent.event.eventDate.loe(criteria.getRangeEnd()));
        }

        if (criteria.isOnlyAvailable()) {
            predicate.and(QEvent.event.participantLimit.eq(0)
                    .or(QEvent.event.participantLimit.gt(QEvent.event.confirmedRequests)));
        }

        Pageable pageable = PageRequest.of(criteria.getFrom() / criteria.getSize(),
                criteria.getSize(),
                criteria.getSort());

        Page<Event> events = eventRepository.findAll(predicate, pageable);
        log.info("Найдены события: {}", events.getTotalElements());

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);

        return events.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    return mapper.eventToShortEventResponseDto(event, userShort);
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

        if (req.getStateAction() == EventStateAction.SEND_TO_REVIEW && updatingEvent.getState() != EventState.CANCELED) {
            updatingEvent.setState(EventState.PENDING);
        }

        if (req.getStateAction() == EventStateAction.CANCEL_REVIEW) {
            updatingEvent.setState(EventState.CANCELED);
        }

        log.info("Событие {} обновлено данными из запроса {}", updatingEvent, req);

        return mapper.eventToEventResponseDto(updatingEvent, getUserShortDto(userId));
    }

    @Override
    public List<AdminEventResponseDto> findAdminEvents(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable) {

        List<Event> events = eventRepository.findAdminEvents(
                users, states, categories, rangeStart, rangeEnd, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);

        return events.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    return mapper.toAdminEventFullDto(event, userShort);
                })
                .collect(Collectors.toList());
    }


    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public EventResponseDto getEventById(Long eventId) {
        Event event = findEvent(eventId);
        return mapper.eventToEventResponseDto(event, getUserShortDto(event.getInitiatorId()));
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

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);
        Map<Long, Long> viewsMap = getViewsForEvents(events.stream().map(Event::getId).collect(Collectors.toList()));

        return events.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, userShort);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
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

        Map<Long, UserShortDto> usersMap = getUsersShortDto(initiatorIds);
        Map<Long, Long> viewsMap = getViewsForEvents(allEvents.stream().map(Event::getId).collect(Collectors.toList()));

        return allEvents.stream()
                .map(event -> {
                    UserShortDto userShort = usersMap.get(event.getInitiatorId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, userShort);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminEventResponseDto updateAdminEvent(Long eventId, UpdateEventAdminRequest req) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " not found"));

        Event updatedEvent = updateEventByAdmin(event, req);
        log.info("Updated eventId: {}", updatedEvent);

        return mapper.toAdminEventFullDto(updatedEvent, getUserShortDto(event.getInitiatorId()));
    }

    @Transactional
    public Event updateEventByAdmin(Event event, UpdateEventAdminRequest update) {

        if (update.getCategory() != null) {
            Category category = categoryRepository.findById(Long.valueOf(update.getCategory()))
                    .orElseThrow(() -> new NoSuchElementException("Category with id " + update.getCategory() + " doesnt exist "));
            event.setCategory(category);
        }

        EventState state = event.getState();
        EventStateAction updateStateAction = update.getStateAction();
        if (updateStateAction == null) {
            updateStateAction = EventStateAction.PUBLISH_EVENT;
        }
        if (updateStateAction == EventStateAction.PUBLISH_EVENT) {
            if (state != EventState.PENDING) {
                throw new ConflictException("Only events with waiting status could be published");
            }
            if (event.getEventDate().minusHours(1L).isBefore(LocalDateTime.now())) {
                throw new ConflictException("Event could be changed only one hour before now");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());

        } else if (updateStateAction == EventStateAction.REJECT_EVENT) {
            if (state == EventState.PUBLISHED) {
                throw new ConflictException("Published eventId could not be rejected");
            }
            event.setState(EventState.REJECTED);

        } else {
            throw new NoSuchElementException("Unknown state action");
        }

        if (update.getTitle() != null) {
            event.setTitle(update.getTitle());
        }

        if (update.getAnnotation() != null) {
            event.setAnnotation(update.getAnnotation());
        }

        if (update.getDescription() != null) {
            event.setDescription(update.getDescription());
        }

        if (update.getEventDate() != null) {
            if (update.getEventDate().isBefore(LocalDateTime.now())) {
                throw new ConflictException("Event date couldnt be in the past");
            }
            event.setEventDate(update.getEventDate());
        }

        if (update.getParticipantLimit() != null) {
            event.setParticipantLimit(update.getParticipantLimit());
        }

        if (update.getLocation() != null) {
            event.setLat(update.getLocation().getLat());
            event.setLon(update.getLocation().getLon());
        }

        if (update.getPaid() != null) {
            event.setPaid(update.getPaid());
        }

        if (update.getRequestModeration() != null) {
            event.setRequestModeration(update.getRequestModeration());
        }

        return event;
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

    private UserShortDto getUserShortDto(Long userId) {
        UserDto userDto = getUserFromClient(userId);
        return UserShortDto.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .build();
    }

    private Map<Long, UserShortDto> getUsersShortDto(Set<Long> userIds) {
        return userIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::getUserShortDto
                ));
    }

    private Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        try {
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = eventIds.stream()
                    .map(id -> "/events/" + id)
                    .collect(Collectors.toList());

            return statsClient.getStats(end.minusYears(1), end, uris, true)
                    .stream()
                    .collect(Collectors.toMap(
                            stat -> Long.parseLong(stat.getUri().replace("/events/", "")),
                            ViewStatsDto::getHits,
                            (a, b) -> a + b
                    ));
        } catch (Exception e) {
            log.error("Error fetching views for events: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " notFound"));
    }

    private void checkPermission(Event event, Long userId) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ResourceAccessException("Access to eventId " + event + " forbidden");
        }
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

    private Long getViews(Long eventId) {
        try {
            LocalDateTime end = LocalDateTime.now();
            List<String> gettingUris = new ArrayList<>();
            gettingUris.add("/events/" + eventId);
            return statsClient.getStats(end.minusYears(1), end, gettingUris, true)
                    .stream()
                    .map(ViewStatsDto::getHits)
                    .reduce(0L, Long::sum);
        } catch (Exception e) {
            return 0L;
        }
    }
}