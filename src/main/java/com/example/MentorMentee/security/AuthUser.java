package com.example.MentorMentee.security;

import com.example.MentorMentee.model.Role;

/** The authenticated principal placed in the security context by {@link JwtAuthFilter}. */
public record AuthUser(String id, String email, String fullName, Role role) {
}
