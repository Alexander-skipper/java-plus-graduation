package ru.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import ru.practicum.util.EventState;

import java.time.LocalDateTime;

@Entity
@Table(name = "events", schema = "public")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @NotBlank
    @Length(min = 3, max = 120)
    private String title;

    @Column
    @NotBlank
    @Length(min = 20)
    private String description;

    @Column
    @NotBlank
    @Length(min = 20, max = 2000)
    private String annotation;

    @Column(name = "event_date")
    @NotNull
    @Future
    private LocalDateTime eventDate;

    @Column(name = "created_on")
    private LocalDateTime createdOn = LocalDateTime.now();

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean paid;

    @Column(name = "participant_limit", columnDefinition = "INT DEFAULT 0")
    @PositiveOrZero
    private Integer participantLimit;

    @Column(name = "request_moderation", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requestModeration;

    @Column
    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double lat;

    @Column
    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double lon;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private EventState state;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "user_id")
    private Long initiatorId;

    @Column(name = "initiator_name")
    private String initiatorName;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "confirmed_requests")
    private int confirmedRequests;

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title=" + title +
                ", description=" + description +
                ", annotation=" + annotation +
                ", eventDate=" + eventDate +
                ", paid=" + paid +
                ", participantLimit=" + participantLimit +
                ", requestModeration=" + requestModeration +
                ", lat=" + lat +
                ", lon=" + lon +
                ", state=" + state +
                "}";
    }
}
