package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;

@FeignClient(
        name = "user-service",
        fallback = UserClientFallback.class
)
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getUser(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/exists/{userId}")
    boolean userExists(@PathVariable("userId") Long userId);
}
