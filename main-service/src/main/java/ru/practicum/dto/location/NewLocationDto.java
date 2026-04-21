package ru.practicum.dto.location;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.util.LocationType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewLocationDto {

    @NotBlank
    @Size(min = 5, max = 50)
    private String name;

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double longitude;

    @PositiveOrZero
    private Double radius;

    @NotNull
    private LocationType type;
}
