package ru.practicum.service.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @Override
    public CategoryDto add(CategoryDto dto) {
        Category entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Category with id=" + id + " was not found");
        }
        repository.deleteById(id);
    }

    @Override
    public CategoryDto update(Long id, CategoryDto dto) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category with id=" + id + " was not found"));

        category.setName(dto.getName());
        return mapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> findAll(Pageable pageable) {
        Page<Category> categoryPage = repository.findAll(pageable);
        return categoryPage.getContent().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto findById(Long catId) {
        Category category = repository.findById(catId)
                .orElseThrow(() -> new NoSuchElementException("Category with id=" + catId + " was not found"));
        return mapper.toDto(category);
    }
}
