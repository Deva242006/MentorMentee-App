package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * An assignment a mentor creates <b>once</b> for their whole cohort. It carries no per-mentee state —
 * each mentee's submission/grade lives in a separate {@link AssignmentSubmission}. Every mentee assigned
 * to {@code mentorId} sees this assignment (including mentees who join later).
 */
@Document(collection = "assignments")
public class Assignment {

    @Id
    private String id;
    private String mentorId;

    private String title;
    private String description;
    private LocalDate dueDate;

    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
