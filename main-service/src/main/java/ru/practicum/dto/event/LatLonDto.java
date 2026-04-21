package ru.practicum.dto.event;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LatLonDto {
    @NotNull
    @Positive
    private double lat;

    @NotNull
    @Positive
    private double lon;
}
