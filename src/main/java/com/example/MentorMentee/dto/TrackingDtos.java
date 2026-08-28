package com.example.MentorMentee.dto;

import com.example.MentorMentee.model.Priority;
import com.example.MentorMentee.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Payloads for assignments, assessments and tasks under the broadcast model: a mentor creates each item
 * once for their whole cohort, mentees act on it, and the mentor reads aggregate progress.
 *
 * <ul>
 *   <li><b>Requests</b> — what a mentor sends to create/update an item, or to grade/score one mentee.</li>
 *   <li><b>Summaries + progress</b> — the mentor's list cards and per-item drill-down (who's done, who's
 *       pending, class average).</li>
 *   <li><b>Mentee views</b> — a shared item joined with <i>that</i> mentee's own status.</li>
 * </ul>
 */
public final class TrackingDtos {

    // --- requests ---

    public record AssignmentRequest(
            @NotBlank String title,
            String description,
            LocalDate dueDate) {}

    public record AssessmentRequest(
            @NotBlank String title,
            String type,
            Double maxScore,
            LocalDate dueDate) {}

    public record TaskRequest(
            @NotBlank String title,
            String description,
            LocalDate dueDate,
            Priority priority) {}

    public record GradeRequest(Double grade, String feedback) {}

    public record ScoreRequest(Double score, String remarks) {}

    public record TaskStatusRequest(@NotNull TaskStatus status) {}

    // --- assignment: mentor summary + progress ---

    public record AssignmentSummary(
            String id,
            String title,
            String description,
            LocalDate dueDate,
            Instant createdAt,
            int totalMentees,
            long submittedCount,
            long gradedCount,
            long pendingCount,
            Double avgGrade) {}

    public record AssignmentProgressRow(
            String menteeId,
            String menteeName,
            String status,
            String submissionDocId,
            String submissionName,
            Instant submittedAt,
            Double grade,
            String feedback) {}

    public record AssignmentProgress(AssignmentSummary summary, List<AssignmentProgressRow> rows) {}

    // --- assessment: mentor summary + progress ---

    public record AssessmentSummary(
            String id,
            String title,
            String type,
            Double maxScore,
            LocalDate dueDate,
            Instant createdAt,
            int totalMentees,
            long scoredCount,
            long pendingCount,
            Double avgScore) {}

    public record AssessmentProgressRow(
            String menteeId,
            String menteeName,
            Double score,
            String remarks,
            Instant scoredAt) {}

    public record AssessmentProgress(AssessmentSummary summary, List<AssessmentProgressRow> rows) {}

    // --- task: mentor summary + progress ---

    public record TaskSummary(
            String id,
            String title,
            String description,
            LocalDate dueDate,
            String priority,
            Instant createdAt,
            int totalMentees,
            long doneCount,
            long inProgressCount,
            long todoCount) {}

    public record TaskProgressRow(
            String menteeId,
            String menteeName,
            String status,
            Instant updatedAt) {}

    public record TaskProgressDetail(TaskSummary summary, List<TaskProgressRow> rows) {}

    // --- mentee-facing views (shared item + this mentee's own status) ---

    public record MenteeAssignmentView(
            String id,
            String title,
            String description,
            LocalDate dueDate,
            String status,
            String submissionDocId,
            String submissionName,
            Double grade,
            String feedback,
            Instant submittedAt) {}

    public record MenteeAssessmentView(
            String id,
            String title,
            String type,
            Double score,
            Double maxScore,
            LocalDate dueDate,
            String remarks,
            Instant scoredAt) {}

    public record MenteeTaskView(
            String id,
            String title,
            String description,
            LocalDate dueDate,
            String priority,
            String status) {}

    private TrackingDtos() {}
}
