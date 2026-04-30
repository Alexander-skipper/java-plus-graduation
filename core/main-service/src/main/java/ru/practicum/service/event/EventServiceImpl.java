package ru.practicum.service.event;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.*;
import ru.practicum.error.ConflictException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.LocationRepository;
import ru.practicum.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventResponseDto create(Long userId, NewEventRequestDto req) {
        User user = findUser(userId);

        Category category = categoryRepository.findById(req.getCategory())
                .orElseThrow(() -> {
                    return new NoSuchElementException("Category with id " + req.getCategory() + " notFound");
                });

        Event newEvent = mapper.eventRequestToEvent(req, category, user);

        Event savedEvent = eventRepository.save(newEvent);
        log.info("Создано новое событие {} от пользователя {}", savedEvent, user);

        return mapper.eventToEventResponseDto(savedEvent, user);
    }

    @Override
    public EventResponseDto get(Long eventId) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> {
                    return new NoSuchElementException("Event with id " + eventId + " notFound");
                });
        log.info("Найдено событие {}", event);

        Long views = getViews(eventId);
        EventResponseDto res = mapper.eventToEventResponseDto(event, event.getInitiator());
        res.setViews(views);

        return res;
    }

    @Override
    public EventResponseDto get(Long userId, Long eventId) {
        User user = findUser(userId);
        Event event = findEvent(eventId);

        log.info("Найдено событие {}", event);
        checkPermission(event, user);

        return mapper.eventToEventResponseDto(event, user);
    }

    @Override
    public List<ShortEventResponseDto> getAll(Long userId, Pageable pageable) {
        User user = findUser(userId);

        return eventRepository.findAllByInitiatorId(userId, pageable)
                .stream()
                .map((event) -> mapper.eventToShortEventResponseDto(event, user))
                .toList();
    }

    @Override
    public List<ShortEventResponseDto> find(EventSearchCriteria criteria) throws Exception {
        BooleanBuilder predicate = new BooleanBuilder();

        if (criteria.hasCategories()) {
            predicate.and(QEvent.event.category.id.in(criteria.getCategories()));
        }

        if (criteria.hasText()) {
            predicate.and(QEvent.event.annotation.contains(criteria.getText()).or(QEvent.event.description.contains(criteria.getText())));
        }

        if (criteria.hasPaid()) {
            predicate.and(QEvent.event.paid.eq(criteria.getPaid()));
        }

        if (criteria.hasRangeStart()) {
            predicate.and(QEvent.event.eventDate.goe(criteria.getRangeStart()));
        }

        if (criteria.hasRangeEnd()) {
            if (criteria.hasRangeStart() && !criteria.getRangeEnd().isAfter(criteria.getRangeStart())) {
                throw new BadRequestException("Invalid rangeEnd");
            }
            predicate.and(QEvent.event.eventDate.loe(criteria.getRangeEnd()));
        }
        if (criteria.isOnlyAvailable()) {
            predicate.and(QEvent.event.participantLimit.eq(0)
                    .or(QEvent.event.participantLimit.gt(QEvent.event.confirmedRequests)));
        }

        Pageable pageable = PageRequest.of(criteria.getFrom() / criteria.getSize(), criteria.getSize(), criteria.getSort());

        Page<Event> events = eventRepository.findAll(predicate, pageable);
        log.info("Найдены события: {}", events);

        return  events.stream()
                .map(mapper::eventToShortEventResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public EventResponseDto update(Long userId, Long eventId, UpdateEventRequestDto req) {
        User user = findUser(userId);
        Event event = findEvent(eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot update published event");
        }

        if (event.getEventDate().minusHours(2L).isBefore(LocalDateTime.now())) {
            throw new ConflictException("Event could be changed only 2 hours before now");
        }

        checkPermission(event, user);
        Category category = null;

        if (req.getCategory() != null) {
            category = categoryRepository.findById(req.getCategory())
                    .orElseThrow(() -> {
                        return new NoSuchElementException("Category with id " + req.getCategory() + " notFound");
                    });
        }

        Event updatingEvent = mapper.updateEventField(event, req, category);

        if (req.getStateAction() == EventStateAction.SEND_TO_REVIEW && updatingEvent.getState() != EventState.CANCELED) {
            updatingEvent.setState(EventState.PENDING);
        }

        if (req.getStateAction() == EventStateAction.CANCEL_REVIEW) {
            updatingEvent.setState(EventState.CANCELED);
        }

        log.info("Сорбытие {} обновлено данными из запроса {}", updatingEvent, req);

        return mapper.eventToEventResponseDto(updatingEvent, user);
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

        return events.stream()
                .map(mapper::toAdminEventFullDto)
                .collect(Collectors.toList());
    }

    //!!!!!!!!FEATURE - 3 ЗАДАНИЕ

    @Override
    public List<ShortEventResponseDto> findEventsByLocation(Long locationId, Pageable pageable) {
        log.info("Finding events for location id: {}", locationId);

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NoSuchElementException("Location with id " + locationId + " not found"));

        List<Event> events = eventRepository.findEventsWithinLocationRadius(
                location.getLatitude(),
                location.getLongitude(),
                location.getRadius() != null ? location.getRadius() : 1.0,
                pageable
        );

        log.info("Found {} events for location id: {} (center: lat={}, lon={}, radius={}km)",
                events.size(), locationId, location.getLatitude(), location.getLongitude(),
                location.getRadius() != null ? location.getRadius() : 1.0);

        return events.stream()
                .map(event -> {
                    Long views = getViews(event.getId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, event.getInitiator());
                    dto.setViews(views);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ShortEventResponseDto> findEventsNear(Double lat, Double lon, Double radius, Pageable pageable) {
        log.info("Finding events for user at coordinates: lat={}, lon={}, radius={}km", lat, lon, radius);

        validateCoordinatesAndRadius(lat, lon, radius);

        List<Location> userLocations = locationRepository.findLocationsContainingPoint(lat, lon);
        if (userLocations.isEmpty()) {
            log.info("User at coordinates lat={}, lon={} doesn't get at any location", lat, lon);
            return Collections.emptyList();
        }
        log.info("User is in {} locations", userLocations.size());

        Set<Event> allEvents = new HashSet<>();

        for (Location location : userLocations) {
            List<Event> eventsInLocation = eventRepository.findEventsWithinLocationRadius(
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getRadius() != null ? location.getRadius() : 1.0,
                    pageable
            );
            allEvents.addAll(eventsInLocation);
            log.info("Found {} events in location id: {}", eventsInLocation.size(), location.getId());
        }
        return allEvents.stream()
                .map(event -> {
                    Long views = getViews(event.getId());
                    ShortEventResponseDto dto = mapper.eventToShortEventResponseDto(event, event.getInitiator());
                    dto.setViews(views);
                    return dto;
                })
                .collect(Collectors.toList());

    }
    //!!!!!!!!FEATURE - 3 ЗАДАНИЕ

    @Override
    @Transactional
    public AdminEventResponseDto updateAdminEvent(Long eventId, UpdateEventAdminRequest req) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " not found"));

        Event updatedEvent = updateEventByAdmin(event, req);
        log.info("Updated event: {}", updatedEvent);

        return mapper.toAdminEventFullDto(updatedEvent);
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
                throw new ConflictException("Published event could not be rejected");
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    return new NoSuchElementException("User with id " + userId + " notFound");
                });
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    return new NoSuchElementException("Event with id " + eventId + " notFound");
                });
    }

    private void checkPermission(Event event, User user) {
        if (!event.getInitiator().equals(user)) {
            throw new ResourceAccessException("Access to event " + event + " forbidden");
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
