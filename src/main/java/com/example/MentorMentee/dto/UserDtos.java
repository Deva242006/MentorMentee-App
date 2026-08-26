package com.example.MentorMentee.dto;

import com.example.MentorMentee.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** User management payloads (admin) and the shared user view. */
public final class UserDtos {

    /** Read model returned to the client. Never carries the password hash. */
    public record UserView(
            String id,
            String fullName,
            String email,
            String role,
            boolean active,
            String mentorId,
            String mentorName,
            String phone,
            String program,
            String year,
            boolean googleConnected,
            Instant createdAt) {}

    public record CreateUserRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
            @NotNull Role role,
            String mentorId,
            String phone,
            String program,
            String year) {}

    public record UpdateUserRequest(
            @NotBlank String fullName,
            Boolean active,
            String mentorId,
            String phone,
            String program,
            String year) {}

    /** {@code mentorId} null clears the assignment. */
    public record AssignMentorRequest(String mentorId) {}

    public record MenteeSummary(
            UserView mentee,
            long assessments,
            long pendingAssignments,
            long openTasks,
            long documents) {}

    public record AdminStats(long mentors, long mentees, long admins, long forms) {}

    private UserDtos() {}
}
