package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/**
 * An assessment a mentor creates <b>once</b> for their whole cohort (a quiz, exam, viva, ...). It defines
 * what is being assessed and out of how many marks; the mentor later records each mentee's mark as an
 * {@link AssessmentScore}. Every mentee assigned to {@code mentorId} is part of this assessment.
 */
@Document(collection = "assessments")
public class Assessment {

    @Id
    private String id;
    private String mentorId;

    private String title;
    private String type;        // e.g. Quiz, Exam, Presentation, Viva
    private Double maxScore;
    private LocalDate dueDate;

    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
