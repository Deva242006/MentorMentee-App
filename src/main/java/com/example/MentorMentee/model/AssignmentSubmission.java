package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One mentee's progress on a shared {@link Assignment}. Created lazily — when the mentee first submits,
 * or when the mentor grades. No record for a (assignment, mentee) pair means the mentee is still PENDING.
 */
@Document(collection = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    private String id;
    private String assignmentId;
    private String menteeId;

    private AssignmentStatus status = AssignmentStatus.PENDING;

    /** id of the {@link DocumentMeta} the mentee uploaded as their submission. */
    private String submissionDocId;
    private Instant submittedAt;

    private Double grade;
    private String feedback;
    private Instant gradedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }

    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }

    public String getSubmissionDocId() { return submissionDocId; }
    public void setSubmissionDocId(String submissionDocId) { this.submissionDocId = submissionDocId; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Instant getGradedAt() { return gradedAt; }
    public void setGradedAt(Instant gradedAt) { this.gradedAt = gradedAt; }
}
