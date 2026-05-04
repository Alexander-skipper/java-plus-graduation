package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;

@FeignClient(
        name = "category-service",
        fallback = CategoryClientFallback.class
)
public interface CategoryClient {

    @GetMapping("/internal/categories/{categoryId}")
    CategoryDto getCategory(@PathVariable("categoryId") Long categoryId);
}
