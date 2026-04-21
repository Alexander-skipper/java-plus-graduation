package ru.practicum.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.exception.TimeRangeException;
import ru.practicum.model.EndpointHit;
import ru.practicum.model.EndpointHitMapper;
import ru.practicum.repository.StatsRepository;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ru.practicum.dto.Const.TIMESTAMP_PATTERN;

@Service
@Slf4j
@AllArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;
    private final EndpointHitMapper endpointHitMapper;

    @Override
    @Transactional
    public EndpointHitDto addHit(EndpointHitDto endpointHitDto) {
        EndpointHit endpointHit = endpointHitMapper.mapToEndpointHit(endpointHitDto);
        return endpointHitMapper.mapToEndpointHitDto(statsRepository.save(endpointHit));
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN);

        start = URLDecoder.decode(start, StandardCharsets.UTF_8);
        end = URLDecoder.decode(end, StandardCharsets.UTF_8);

        LocalDateTime startTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endTime = LocalDateTime.parse(end, formatter);

        if (endTime.isBefore(startTime)) {
            throw new TimeRangeException("End time is before start time");
        }

        if (uris == null || uris.isEmpty()) {
            return unique ? statsRepository.getUniqueStats(startTime, endTime) : statsRepository.getNotUniqueStats(startTime, endTime);
        }

        return unique ? statsRepository.getUniqueStatsWithUris(startTime, endTime, uris) : statsRepository.getNotUniqueStatsWithUris(startTime, endTime, uris);
    }
}
