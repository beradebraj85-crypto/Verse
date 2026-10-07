package com.debraj.verse.dto;

import com.debraj.verse.entity.Role;

public record RegisterUserRequest(
        String username,
        String email,
        String password
) {
}

