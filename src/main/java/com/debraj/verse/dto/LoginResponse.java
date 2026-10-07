package com.debraj.verse.dto;

public record LoginResponse(
        String accessToken,
        UserDto user
) {
}

