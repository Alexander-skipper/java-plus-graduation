package ru.practicum.service.category;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto add(CategoryDto dto);

    void delete(Long id);

    CategoryDto update(Long id, CategoryDto dto);

    List<CategoryDto> findAll(Pageable pageable);

    CategoryDto findById(Long catId);
}
