package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class DateTimeProperties {

    @Getter
    @Setter
    private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
}
