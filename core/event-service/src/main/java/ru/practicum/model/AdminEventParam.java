package ru.practicum.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.util.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.util.Patterns.TIMESTAMP_PATTERN;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminEventParam {
    private List<Long> users;
    private List<EventState> states;
    private List<Long> categories;
    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime rangeStart;
    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime rangeEnd;
}
