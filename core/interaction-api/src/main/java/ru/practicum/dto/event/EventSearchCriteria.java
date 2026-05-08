package ru.practicum.dto.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Collection;

import static ru.practicum.util.Patterns.EVENTS_SORT_PATTERN;
import static ru.practicum.util.Patterns.TIMESTAMP_PATTERN;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSearchCriteria {

    @Size(max = 100, message = "Текст поиска не должен превышать 100 символов")
    private String text;

    private Collection<Long> categories;
    private Boolean paid;

    @DateTimeFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime rangeEnd;

    @Builder.Default
    private Boolean onlyAvailable = false;

    @Pattern(regexp = EVENTS_SORT_PATTERN, message = "Допустимые значения: EVENT_DATE, VIEWS")
    private String sort;

    @Min(0)
    @Builder.Default
    private Integer from = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 10;

    public boolean hasText() {
        return text != null && !text.trim().isBlank();
    }

    public boolean hasCategories() {
        return categories != null && !categories.isEmpty();
    }

    public boolean hasPaid() {
        return paid != null;
    }

    public boolean hasRangeStart() {
        return rangeStart != null;
    }

    public boolean hasRangeEnd() {
        return rangeEnd != null;
    }

    public boolean isOnlyAvailable() {
        return onlyAvailable != null && onlyAvailable;
    }

    public Sort getSort() {
        if (sort == null) {
            return Sort.by("eventDate").descending();
        }
        return switch (sort) {
            case "EVENT_DATE" -> Sort.by("eventDate").descending();
            case "VIEWS" -> Sort.by("views").descending();
            default -> Sort.by("eventDate").descending();
        };
    }
}