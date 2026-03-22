package com.webdev.greenify.service;

import com.webdev.greenify.dto.UserDto;
import java.util.List;

public interface UserService {
    List<UserDto> findAllUsers();
    UserDto findUserById(Long id);
}
