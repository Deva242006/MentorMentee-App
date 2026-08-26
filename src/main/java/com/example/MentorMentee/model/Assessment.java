package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

/** A scored evaluation recorded by a mentor for one mentee. */
@Document(collection = "assessments")
public class Assessment {

    @Id
    private String id;
    private String menteeId;
    private String mentorId;

    private String title;
    private String type;        // e.g. Quiz, Exam, Presentation, Viva
    private Double score;
    private Double maxScore;
    private LocalDate assessedOn;
    private String remarks;

    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public LocalDate getAssessedOn() { return assessedOn; }
    public void setAssessedOn(LocalDate assessedOn) { this.assessedOn = assessedOn; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
