package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.category.CategoryDto;

@Slf4j
@Component
public class CategoryClientFallback implements CategoryClient {

    @Override
    public CategoryDto getCategory(Long categoryId) {
        log.warn("Category-service unavailable, returning fallback for categoryId: {}", categoryId);

        CategoryDto fallback = new CategoryDto();
        fallback.setId(categoryId);
        fallback.setName("Default Category");
        return fallback;
    }
}
