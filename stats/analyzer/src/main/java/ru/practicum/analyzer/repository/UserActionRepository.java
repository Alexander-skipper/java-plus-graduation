package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserActionEntity;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserActionEntity, Long> {

    Optional<UserActionEntity> findByUserIdAndEventId(Long userId, Long eventId);

    List<UserActionEntity> findAllByUserId(Long userId);

    @Query("SELECT u.eventId FROM UserActionEntity u WHERE u.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT u.eventId, SUM(u.weight) FROM UserActionEntity u " +
            "WHERE u.eventId IN :eventIds " +
            "GROUP BY u.eventId")
    List<Object[]> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);
}
