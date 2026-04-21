package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.user.NewUserRequestDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User mapNewUserRequestDtoToUser(NewUserRequestDto newUserRequestDto);

    NewUserRequestDto mapUserToNewUserRequestDto(User user);

    UserDto mapUserToUserDto(User user);

    UserShortDto userToUserShortDto(User user);

}
