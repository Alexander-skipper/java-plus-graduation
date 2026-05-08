package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ru.practicum.util.Patterns.TIMESTAMP_PATTERN;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewEventRequestDto {

    @NotBlank(message = "Annotation cannot be blank")
    @Size(min = 20, max = 2000, message = "Annotation length must be between 20 and 2000")
    private String annotation;

    @NotNull(message = "Category cannot be null")
    private Long category;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 20, max = 7000, message = "Description length must be between 20 and 7000")
    private String description;

    @NotNull(message = "Event date cannot be null")
    @Future(message = "Event date must be in the future")
    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime eventDate;

    @NotNull(message = "Location cannot be null")
    private LatLonDto location;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 120, message = "Title length must be between 3 and 120")
    private String title;

    @Builder.Default
    private Boolean paid = false;

    @Builder.Default
    @PositiveOrZero(message = "Participant limit must be positive or zero")
    private Integer participantLimit = 0;

    @Builder.Default
    private Boolean requestModeration = true;
}