package ru.practicum.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
public class NewUserRequestDto {
    @Email
    @Length(min = 6, max = 254)
    @NotNull
    private String email;

    @Length(min = 2, max = 250)
    @NotBlank
    private String name;
}
