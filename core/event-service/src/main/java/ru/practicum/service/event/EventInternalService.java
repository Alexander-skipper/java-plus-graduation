package ru.practicum.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.Event;
import ru.practicum.repository.EventRepository;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventInternalService {

    private final EventRepository eventRepository;

    public void incrementConfirmedRequests(Long eventId) {
        Event event = findEvent(eventId);
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        eventRepository.save(event);
        log.info("Incremented confirmed requests for event {}", eventId);
    }

    public void decrementConfirmedRequests(Long eventId) {
        Event event = findEvent(eventId);
        event.setConfirmedRequests(event.getConfirmedRequests() - 1);
        eventRepository.save(event);
        log.info("Decremented confirmed requests for event {}", eventId);
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " not found"));
    }
}
