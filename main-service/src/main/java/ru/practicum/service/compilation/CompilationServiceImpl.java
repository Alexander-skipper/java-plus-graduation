package ru.practicum.service.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.model.Event;
import ru.practicum.repository.EventRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository repository;
    private final EventRepository eventRepository;
    private final CompilationMapper mapper;

    @Override
    public CompilationDto add(NewCompilationDto dto) {

        Set<Event> events = dto.getEvents() == null ?
                Set.of() :
                eventRepository.findAllById(dto.getEvents())
                        .stream().collect(Collectors.toSet());

        Compilation comp = Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned())
                .events(events)
                .build();

        return mapper.toDto(repository.save(comp));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Compilation with id=" + id + " was not found");
        }
        repository.deleteById(id);
    }

    @Override
    public CompilationDto update(Long id, UpdateCompilationRequest dto) {

        Compilation comp = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Compilation with id=" + id + " was not found"));

        if (dto.getTitle() != null && !dto.getTitle().isBlank())
            comp.setTitle(dto.getTitle());

        if (dto.getPinned() != null)
            comp.setPinned(dto.getPinned());

        if (dto.getEvents() != null)
            comp.setEvents(
                    eventRepository.findAllById(dto.getEvents())
                            .stream().collect(Collectors.toSet())
            );

        return mapper.toDto(comp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> findAll(Boolean pinned, Pageable pageable) {
        if (pinned == null) {
            return repository.findAll(pageable)
                    .stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
        } else {
            return repository.findByPinned(pinned, pageable)
                    .stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto findById(Long compId) {
        Compilation comp = repository.findById(compId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Compilation with id=" + compId + " was not found"));
        return mapper.toDto(comp);
    }
}
