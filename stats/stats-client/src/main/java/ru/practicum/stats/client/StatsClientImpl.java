package ru.practicum.stats.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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

@Component
public class StatsClientImpl implements StatsClient {

    private final RestClient restClient;
    private final String serverUrl;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

    public StatsClientImpl(@Value("${server.url:http://localhost:9090}") String serverUrl) {
        this.restClient = RestClient.create();
        this.serverUrl = serverUrl;
    }

    @Override
    public void hit(EndpointHitDto hit) {
        String uri = UriComponentsBuilder.newInstance()
                .uri(URI.create(serverUrl))
                .path("/hit")
                .toUriString();

        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(hit)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start,
                                       LocalDateTime end,
                                       List<String> uris,
                                       boolean unique
    ) {
        String uriWithParams = UriComponentsBuilder.newInstance()
                .uri(URI.create(serverUrl))
                .path("/stats")
                .queryParam("start", start.format(formatter))
                .queryParam("end", end.format(formatter))
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .toUriString();

        ViewStatsDto[] response = restClient.get()
                .uri(uriWithParams)
                .retrieve()
                .body(ViewStatsDto[].class);

        return response == null
                ? List.of()
                : Arrays.asList(response);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
