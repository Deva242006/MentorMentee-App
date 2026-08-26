package com.example.MentorMentee.dto;

import jakarta.validation.constraints.NotBlank;

/** Login request/response payloads. */
public final class AuthDtos {

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record LoginResponse(String token, UserDtos.UserView user) {}

    private AuthDtos() {}
}
