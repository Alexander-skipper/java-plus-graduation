package ru.practicum.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShortLocationResponseDto {
    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
}
