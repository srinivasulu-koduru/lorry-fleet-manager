package com.lorryowner.backend.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String username,
        String role,
        String fullName
) {
    public LoginResponse(String accessToken, long expiresIn, Long userId, String username, String role, String fullName) {
        this(accessToken, "Bearer", expiresIn, userId, username, role, fullName);
    }
}
