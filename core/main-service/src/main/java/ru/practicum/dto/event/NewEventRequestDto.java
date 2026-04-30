package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotBlank
    private String annotation;

    private Long category;

    @NotBlank
    private String description;

    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime eventDate;

    private LatLonDto location;

    @NotBlank
    private String title;

    @NotNull
    @Builder.Default
    private Boolean paid = false;

    @PositiveOrZero
    @Builder.Default
    private Integer participantLimit = 0;

    @NotNull
    private Boolean requestModeration = true;
}
