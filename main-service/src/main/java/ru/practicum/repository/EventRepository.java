package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.Event;
import ru.practicum.util.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Optional<Event> findByIdAndState(Long eventId, EventState stateAction);

    @Query("""
            SELECT e FROM Event e WHERE (:users IS NULL OR e.initiator.id IN :users)
               AND (:states IS NULL OR e.state IN :states)
               AND (:categories IS NULL OR e.category.id IN :categories)
               AND e.eventDate >= COALESCE(:rangeStart, e.eventDate)
               AND e.eventDate <= COALESCE(:rangeEnd, e.eventDate)
            """)
    List<Event> findAdminEvents(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );

    @Query(value = """
        SELECT e.* FROM events e
        WHERE distance(:lat, :lon, e.lat, e.lon) <= :radius
        AND e.state = 'PUBLISHED'
        ORDER BY e.event_date ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM events e
        WHERE distance(:lat, :lon, e.lat, e.lon) <= :radius
        AND e.state = 'PUBLISHED'
        """,
            nativeQuery = true)
    List<Event> findEventsWithinLocationRadius(@Param("lat") Double lat,
                                       @Param("lon") Double lon,
                                       @Param("radius") Double radius,
                                       Pageable pageable);
}
