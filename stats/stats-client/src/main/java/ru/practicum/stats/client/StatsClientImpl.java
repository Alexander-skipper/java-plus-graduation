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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(hit)
                .retrieve()
                .toBodilessEntity();
        log.debug("Запрос hit отправлен в stats-server: {}", uri);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", URLEncoder.encode(start.format(formatter), StandardCharsets.UTF_8))
                .queryParam("end", URLEncoder.encode(end.format(formatter), StandardCharsets.UTF_8))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String uri : uris) {
                builder.queryParam("uris", URLEncoder.encode(uri, StandardCharsets.UTF_8));
            }
        }

        URI fullUri = makeUri(builder.toUriString());

        ViewStatsDto[] response = restClient.get()
                .uri(fullUri)
                .retrieve()
                .body(ViewStatsDto[].class);

        log.debug("Получена статистика от stats-server: {}", fullUri);

        return response == null ? List.of() : Arrays.asList(response);
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            if (instances == null || instances.isEmpty()) {
                throw new RuntimeException("Экземпляры сервиса не найдены для: " + statsServiceId);
            }
            return instances.getFirst();
        } catch (Exception exception) {
            throw new RuntimeException("Ошибка обнаружения сервиса статистики с id: " + statsServiceId, exception);
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }
}