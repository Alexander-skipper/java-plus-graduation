package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.user.UserShortDto;

import java.time.LocalDateTime;

import static ru.practicum.util.Patterns.TIMESTAMP_PATTERN;

@Data
@Builder
public class ShortEventResponseDto {

    private String annotation;

    private CategoryDto category;

    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime eventDate;

    private UserShortDto initiator;

    private LatLonDto location;

    private Boolean paid;

    private String title;

    private Integer confirmedRequests;

    private Long id;

    private Double rating;
}
