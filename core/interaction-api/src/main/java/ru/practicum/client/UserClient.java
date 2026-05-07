package ru.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.NewUserRequestDto;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/admin/users")
    List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                           @RequestParam(defaultValue = "0") int from,
                           @RequestParam(defaultValue = "10") int size);

    @PostMapping("/admin/users")
    UserDto createUser(@RequestBody NewUserRequestDto userRequestDto);

    @DeleteMapping("/admin/users/{userId}")
    void deleteUser(@PathVariable Long userId);

    @GetMapping("/internal/users/{userId}")
    UserDto getUserById(@PathVariable Long userId);

    @GetMapping("/internal/users/{userId}/exists")
    Boolean userExists(@PathVariable Long userId);
}
