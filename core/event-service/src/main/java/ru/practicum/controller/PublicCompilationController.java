package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.service.compilation.CompilationService;

import java.util.List;

@RestController
@RequestMapping("/compilations")
@Slf4j
@RequiredArgsConstructor
public class PublicCompilationController {
    private final CompilationService compilationService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CompilationDto> findAll(@RequestParam(required = false) Boolean pinned,
                                        @RequestParam(defaultValue = "0") int from,
                                        @RequestParam(defaultValue = "10") int size) {
        return compilationService.findAll(pinned, PageRequest.of(from / size, size));
    }

    @GetMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto findById(@PathVariable Long compId) {
        log.info("Request for compilation by id {}", compId);
        return compilationService.findById(compId);
    }

}
