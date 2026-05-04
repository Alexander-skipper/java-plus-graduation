package ru.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;

@Slf4j
@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto getUser(Long userId) {
        log.warn("User-service unavailable, returning fallback for userId: {}", userId);

        return UserDto.builder()
                .id(userId)
                .name("Unknown User")
                .email("unknown@example.com")
                .build();
    }

    @Override
    public boolean userExists(Long userId) {
        log.warn("User-service unavailable, assuming user exists for userId: {}", userId);

        return true;
    }
}
