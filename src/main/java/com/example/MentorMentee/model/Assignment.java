package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/** Submittable work assigned by a mentor. The mentee submits a document, the mentor grades it. */
@Document(collection = "assignments")
public class Assignment {

    @Id
    private String id;
    private String menteeId;
    private String mentorId;

    private String title;
    private String description;
    private LocalDate dueDate;
    private AssignmentStatus status = AssignmentStatus.PENDING;

    /** id of the {@link DocumentMeta} the mentee uploaded as their submission. */
    private String submissionDocId;
    private Double grade;
    private String feedback;

    private Instant createdAt = Instant.now();
    private Instant submittedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }

    public String getSubmissionDocId() { return submissionDocId; }
    public void setSubmissionDocId(String submissionDocId) { this.submissionDocId = submissionDocId; }

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
