package ru.practicum.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequestDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional(readOnly = true)

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getUsers(List<Long> ids, Pageable pageable) {
        log.debug("Get users by ids {}", ids);
        if (ids == null || ids.isEmpty()) {
            return userRepository.findAll(pageable)
                    .stream()
                    .map(userMapper::mapUserToUserDto)
                    .collect(Collectors.toList());
        }
        return userRepository.findAllByIdIn(ids, pageable)
                .stream()
                .map(userMapper::mapUserToUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto addUser(NewUserRequestDto newUserRequestDto) {
        log.debug("Add user {}", newUserRequestDto);
        User user = userMapper.mapNewUserRequestDtoToUser(newUserRequestDto);
        return userMapper.mapUserToUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        log.debug("Delete user {}", userId);
        if (!userRepository.existsById(userId)) {
            log.info("User with id: {} was not found", userId);
            throw new NoSuchElementException("User not found");
        }
        userRepository.deleteById(userId);
        log.info("User with id: {} was deleted", userId);
    }
}
