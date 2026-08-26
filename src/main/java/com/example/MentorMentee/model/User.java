package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A person who can log in. Admins provision users and assign each mentee to a mentor.
 * Mentee-specific fields ({@code mentorId}, {@code program}, ...) are only meaningful when
 * {@code role == MENTEE}; {@code googleConnected} only when {@code role == MENTOR}.
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role;

    private boolean active = true;

    /** For mentees: id of the assigned mentor. */
    private String mentorId;

    /** For mentors: whether a Google account has been linked for the Forms feature. */
    private boolean googleConnected;

    // --- optional mentee profile fields ---
    private String phone;
    private String program;
    private String year;

    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public boolean isGoogleConnected() { return googleConnected; }
    public void setGoogleConnected(boolean googleConnected) { this.googleConnected = googleConnected; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
