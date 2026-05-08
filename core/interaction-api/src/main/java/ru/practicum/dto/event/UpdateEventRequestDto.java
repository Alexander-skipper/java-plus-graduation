package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.practicum.util.EventStateAction;

import java.time.LocalDateTime;

import static ru.practicum.util.Patterns.TIMESTAMP_PATTERN;

@Data
public class UpdateEventRequestDto {

    @Size(min = 3, max = 120, message = "Title length must be between 3 and 120")
    private String title;

    @Size(min = 20, max = 2000, message = "Annotation length must be between 20 and 2000")
    private String annotation;

    @Size(min = 20, max = 7000, message = "Description length must be between 20 and 7000")
    private String description;

    private Long category;

    @Future(message = "Event date must be in the future")
    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime eventDate;

    private LatLonDto location;

    private Boolean paid;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    private Integer participantLimit;

    private Boolean requestModeration;

    private EventStateAction stateAction;
}