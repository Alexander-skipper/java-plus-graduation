package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.NewUserRequestDto;
import ru.practicum.dto.user.UserDto;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.warn("User service is unavailable. Returning empty list.");
        return Collections.emptyList();
    }

    @Override
    public UserDto createUser(NewUserRequestDto userRequestDto) {
        log.error("User service is unavailable. Cannot create user.");
        throw new RuntimeException("User service is unavailable");
    }

    @Override
    public void deleteUser(Long userId) {
        log.error("User service is unavailable. Cannot delete user.");
        throw new RuntimeException("User service is unavailable");
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.error("User service is unavailable. Cannot get user by id {}.", userId);
        throw new RuntimeException("User service is unavailable");
    }

    @Override
    public Boolean userExists(Long userId) {
        log.error("User service is unavailable. Cannot check user existence.");
        throw new RuntimeException("User service is unavailable");
    }
}