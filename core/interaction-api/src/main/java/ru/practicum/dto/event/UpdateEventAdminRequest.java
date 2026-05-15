package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import ru.practicum.util.EventStateAction;

import java.time.LocalDateTime;

import static ru.practicum.util.Patterns.TIMESTAMP_PATTERN;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventAdminRequest {

    @Length(min = 3, max = 120)
    private String title;

    @Length(min = 20, max = 2000)
    private String annotation;

    @Length(min = 20, max = 7000)
    private String description;

    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    @Future
    private LocalDateTime eventDate;

    @PositiveOrZero
    private Integer participantLimit;

    private Long category;
    private LatLonDto location;
    private Boolean paid;
    private Boolean requestModeration;
    private EventStateAction stateAction;
}
