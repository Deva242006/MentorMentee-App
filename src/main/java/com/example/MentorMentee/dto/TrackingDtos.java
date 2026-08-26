package com.example.MentorMentee.dto;

import com.example.MentorMentee.model.Priority;
import com.example.MentorMentee.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;

/** Request/response payloads for assessments, assignments and tasks. */
public final class TrackingDtos {

    // --- Assessments ---
    public record AssessmentRequest(
            @NotBlank String title,
            String type,
            Double score,
            Double maxScore,
            LocalDate assessedOn,
            String remarks) {}

    public record AssessmentView(
            String id,
            String menteeId,
            String title,
            String type,
            Double score,
            Double maxScore,
            LocalDate assessedOn,
            String remarks,
            Instant createdAt) {}

    // --- Assignments ---
    public record AssignmentRequest(
            @NotBlank String title,
            String description,
            LocalDate dueDate) {}

    public record AssignmentView(
            String id,
            String menteeId,
            String title,
            String description,
            LocalDate dueDate,
            String status,
            String submissionDocId,
            String submissionName,
            Double grade,
            String feedback,
            Instant createdAt,
            Instant submittedAt) {}

    public record GradeRequest(Double grade, String feedback) {}

    // --- Tasks ---
    public record TaskRequest(
            @NotBlank String title,
            String description,
            LocalDate dueDate,
            Priority priority) {}

    public record TaskView(
            String id,
            String menteeId,
            String title,
            String description,
            LocalDate dueDate,
            String priority,
            String status,
            Instant createdAt) {}

    public record TaskStatusRequest(@NotNull TaskStatus status) {}

    private TrackingDtos() {}
}
