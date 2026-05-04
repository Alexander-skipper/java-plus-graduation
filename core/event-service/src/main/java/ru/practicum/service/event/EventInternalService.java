package ru.practicum.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.Event;
import ru.practicum.repository.EventRepository;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventInternalService {

    private final EventRepository eventRepository;

    @Transactional
    public void incrementConfirmedRequests(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " not found"));
        event.setConfirmedRequests(event.getConfirmedRequests() + 1);
        eventRepository.save(event);
        log.info("Incremented confirmed requests for event {} to {}", eventId, event.getConfirmedRequests());
    }

    @Transactional
    public void decrementConfirmedRequests(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event with id " + eventId + " not found"));
        event.setConfirmedRequests(Math.max(0, event.getConfirmedRequests() - 1));
        eventRepository.save(event);
        log.info("Decremented confirmed requests for event {} to {}", eventId, event.getConfirmedRequests());
    }
}
