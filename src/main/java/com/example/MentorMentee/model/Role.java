package com.example.MentorMentee.model;

/** Application roles. Stored on {@link User} and encoded into the JWT as a {@code role} claim. */
public enum Role {
    ADMIN, MENTOR, MENTEE
}
