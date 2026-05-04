package ru.practicum.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventRequestStatusUpdateResult {

    @Builder.Default
    private List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
    @Builder.Default
    private List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
}
