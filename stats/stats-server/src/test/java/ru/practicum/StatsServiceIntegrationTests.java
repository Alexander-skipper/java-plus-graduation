package ru.practicum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;
import ru.practicum.service.StatsServiceImpl;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StatsServiceIntegrationTests {

    @Autowired
    private StatsServiceImpl statsService;

    @Autowired
    private StatsRepository statsRepository;

    @BeforeEach
    void setUp() {
        statsRepository.deleteAll();
    }

    @Test
    void shouldAddHitAndReturnDto() {

        LocalDateTime timestamp = LocalDateTime.parse("2025-12-19T10:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        EndpointHitDto inputDto = EndpointHitDto.builder()
                .app("testApp")
                .uri("/test")
                .ip("127.0.0.1")
                .timestamp(timestamp)
                .build();

        EndpointHitDto resultDto = statsService.addHit(inputDto);
        EndpointHit savedEntity = statsRepository.findById(resultDto.getId())
                .orElseThrow(() -> new AssertionError("Entity not found in repository"));

        assertThat(resultDto.getApp()).isEqualTo(inputDto.getApp());
        assertThat(resultDto.getUri()).isEqualTo(inputDto.getUri());
        assertThat(resultDto.getIp()).isEqualTo(inputDto.getIp());
        assertThat(resultDto.getTimestamp()).isEqualTo(inputDto.getTimestamp());

        assertThat(savedEntity.getApp()).isEqualTo(inputDto.getApp());
        assertThat(savedEntity.getUri()).isEqualTo(inputDto.getUri());
        assertThat(savedEntity.getIp()).isEqualTo(inputDto.getIp());
        assertThat(savedEntity.getTimestamp()).isEqualTo(inputDto.getTimestamp());

        assertThat(resultDto.getId()).isNotNull();
        assertThat(savedEntity.getId()).isNotNull();
    }

    @Test
    void shouldGetNotUniqueStatsWithoutUris() {
        statsRepository.save(EndpointHit.builder()
                .app("app1")
                .uri("/page1")
                .ip("192.168.0.1")
                .timestamp(LocalDateTime.of(2025, 12, 19, 10, 0))
                .build());

        statsRepository.save(EndpointHit.builder()
                .app("app1")
                .uri("/page1")
                .ip("192.168.0.2")
                .timestamp(LocalDateTime.of(2025, 12, 19, 11, 0))
                .build());

        List<ViewStatsDto> stats = statsService.getStats("2025-12-19 00:00:00", "2025-12-19 23:59:59", null, false);

        assertThat(stats).hasSize(1);
        ViewStatsDto dto = stats.getFirst();
        assertThat(dto.getApp()).isEqualTo("app1");
        assertThat(dto.getUri()).isEqualTo("/page1");
        assertThat(dto.getHits()).isEqualTo(2L);
    }

    @Test
    void shouldGetUniqueStatsWithoutUris() {
        statsRepository.save(EndpointHit.builder()
                .app("app2")
                .uri("/page2")
                .ip("10.0.0.1")
                .timestamp(LocalDateTime.of(2025, 12, 19, 10, 0))
                .build());

        statsRepository.save(EndpointHit.builder()
                .app("app2")
                .uri("/page2")
                .ip("10.0.0.1")
                .timestamp(LocalDateTime.of(2025, 12, 19, 11, 0))
                .build());

        statsRepository.save(EndpointHit.builder()
                .app("app2")
                .uri("/page2")
                .ip("10.0.0.2")
                .timestamp(LocalDateTime.of(2025, 12, 19, 12, 0))
                .build());

        List<ViewStatsDto> stats = statsService.getStats("2025-12-19 00:00:00", "2025-12-19 23:59:59", null, true);

        assertThat(stats).hasSize(1);
        ViewStatsDto dto = stats.getFirst();
        assertThat(dto.getApp()).isEqualTo("app2");
        assertThat(dto.getUri()).isEqualTo("/page2");
        assertThat(dto.getHits()).isEqualTo(2L);
    }

    @Test
    void shouldGetNotUniqueStatsWithUris() {
        statsRepository.save(EndpointHit.builder()
                .app("app3")
                .uri("/target")
                .ip("1.1.1.1")
                .timestamp(LocalDateTime.of(2025, 12, 19, 10, 0))
                .build());

        statsRepository.save(EndpointHit.builder()
                .app("app3")
                .uri("/other")
                .ip("2.2.2.2")
                .timestamp(LocalDateTime.of(2025, 12, 19, 11, 0))
                .build());

        List<String> uris = List.of("/target");

        List<ViewStatsDto> stats = statsService.getStats("2025-12-19 00:00:00", "2025-12-19 23:59:59", uris, false);

        assertThat(stats).hasSize(1);
        ViewStatsDto dto = stats.getFirst();
        assertThat(dto.getUri()).isEqualTo("/target");
        assertThat(dto.getHits()).isEqualTo(1L);
    }

    @Test
    void shouldGetUniqueStatsWithUris() {
        LocalDateTime baseTime = LocalDateTime.of(2025, 12, 19, 10, 0);

        statsRepository.save(EndpointHit.builder()
                .app("testApp")
                .uri("/target-page")
                .ip("192.168.1.100")
                .timestamp(baseTime)
                .build());

        statsRepository.save(EndpointHit.builder()
                .app("testApp")
                .uri("/target-page")
                .ip("192.168.1.101")
                .timestamp(baseTime.plusHours(1))
                .build());

        statsRepository.save(EndpointHit.builder()
                .app("testApp")
                .uri("/target-page")
                .ip("192.168.1.100")
                .timestamp(baseTime.plusHours(2))
                .build());

        statsRepository.save(EndpointHit.builder()
                .app("testApp")
                .uri("/other-page")
                .ip("192.168.1.200")
                .timestamp(baseTime.plusHours(3))
                .build());

        String start = "2025-12-19 00:00:00";
        String end = "2025-12-19 23:59:59";
        List<String> uris = List.of("/target-page");
        boolean unique = true;

        List<ViewStatsDto> stats = statsService.getStats(start, end, uris, unique);

        assertThat(stats).hasSize(1);

        ViewStatsDto result = stats.getFirst();

        assertThat(result.getApp()).isEqualTo("testApp");
        assertThat(result.getUri()).isEqualTo("/target-page");
        assertThat(result.getHits()).isEqualTo(2L);
    }

}
