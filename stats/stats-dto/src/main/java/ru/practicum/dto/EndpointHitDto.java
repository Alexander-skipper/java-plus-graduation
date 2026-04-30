package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDateTime;

import static ru.practicum.dto.Const.TIMESTAMP_PATTERN;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointHitDto {


    private Long id;

    @NotNull
    @NotBlank(message = "Название приложения не может быть пустым")
    private String app;

    @NotNull
    @NotBlank(message = "URI не может быть пустым")
    private String uri;

    @NotNull
    @NotBlank(message = "IP-адрес не может быть пустым")
    private String ip;

    @NotNull(message = "Время запроса не может быть пустым")
    @PastOrPresent(message = "Время запроса не может быть в будущем")
    @JsonFormat(pattern = TIMESTAMP_PATTERN)
    private LocalDateTime timestamp;
}