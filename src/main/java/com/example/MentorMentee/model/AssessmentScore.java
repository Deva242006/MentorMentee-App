package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One mentee's mark on a shared {@link Assessment}, recorded by the mentor. Created lazily when the mentor
 * enters the score. No record for a (assessment, mentee) pair means the mentee has not been scored yet.
 */
@Document(collection = "assessment_scores")
public class AssessmentScore {

    @Id
    private String id;
    private String assessmentId;
    private String menteeId;

    private Double score;
    private String remarks;
    private Instant scoredAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAssessmentId() { return assessmentId; }
    public void setAssessmentId(String assessmentId) { this.assessmentId = assessmentId; }

    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getScoredAt() { return scoredAt; }
    public void setScoredAt(Instant scoredAt) { this.scoredAt = scoredAt; }
}
