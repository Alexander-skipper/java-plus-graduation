package ru.practicum.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "action.weights")
public class ActionWeightsConfig {
    private double view = 0.4;
    private double register = 0.8;
    private double like = 1.0;
}
