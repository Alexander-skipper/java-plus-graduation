package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.util.ParticipationRequestStatus;

import java.sql.Timestamp;

@Entity
@Table (name = "participation_requests")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Timestamp created;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    private ParticipationRequestStatus status;
}
