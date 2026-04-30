package ru.practicum.service.user;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.user.NewUserRequestDto;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    UserDto addUser(NewUserRequestDto userRequestDto);

    void delete(Long userId);

}
