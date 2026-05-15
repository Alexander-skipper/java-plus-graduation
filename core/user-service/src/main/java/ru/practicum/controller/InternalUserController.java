package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.service.UserService;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
@Slf4j
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserDto getUser(@PathVariable Long userId) {
        log.info("Internal request: get user by id {}", userId);
        return userService.getUserById(userId);
    }

    @GetMapping("/exists/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public boolean userExists(@PathVariable Long userId) {
        log.info("Internal request: check if user exists {}", userId);
        return userService.userExists(userId);
    }
}
