package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static ru.practicum.dto.Const.TIMESTAMP_PATTERN;

@Slf4j
@Component
public class StatsClientImpl implements StatsClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

    public StatsClientImpl(
            DiscoveryClient discoveryClient,
            @Value("${stats.service.id:stats-server}") String statsServiceId
    ) {
        this.restClient = RestClient.create();
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;

        this.retryTemplate = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
    }

    @Override
    public void hit(EndpointHitDto hit) {
        URI uri = makeUri("/hit");
        try {
            restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(hit)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Запрос hit отправлен в stats-server: {}, тело: {}", uri, hit);
        } catch (Exception e) {
            log.error("Ошибка при отправке hit в stats-server: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось отправить hit в сервис статистики", e);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", uri);
            }
        }

        URI fullUri = makeUri(builder.toUriString());

        try {
            ViewStatsDto[] response = restClient.get()
                    .uri(fullUri)
                    .retrieve()
                    .body(ViewStatsDto[].class);

            log.debug("Получена статистика от stats-server: {}, ответ: {}", fullUri,
                    response == null ? "[]" : Arrays.toString(response));

            return response == null ? List.of() : Arrays.asList(response);
        } catch (Exception e) {
            log.error("Ошибка при получении статистики из stats-server: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private ServiceInstance getInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("Экземпляры сервиса не найдены для: " + statsServiceId);
        }
        ServiceInstance instance = instances.get(0);
        log.debug("Найден инстанс {}: {}:{}", statsServiceId, instance.getHost(), instance.getPort());
        return instance;
    }


    private URI makeUri(String path) {
        return retryTemplate.execute(ctx -> {
            ServiceInstance instance = getInstance();
            String fullPath = path.startsWith("/") ? path : "/" + path;
            URI uri = URI.create("http://" + instance.getHost() + ":" + instance.getPort() + fullPath);
            log.debug("Создан URI: {}", uri);
            return uri;
        });
    }
}