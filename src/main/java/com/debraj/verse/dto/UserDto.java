package com.debraj.verse.dto;

import com.debraj.verse.entity.Role;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role
) {
}

