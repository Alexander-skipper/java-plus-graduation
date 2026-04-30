package ru.practicum.service.compilation;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto add(NewCompilationDto dto);

    void delete(Long id);

    CompilationDto update(Long id, UpdateCompilationRequest dto);

    List<CompilationDto> findAll(Boolean pinned, Pageable pageable);

    CompilationDto findById(Long compId);
}
