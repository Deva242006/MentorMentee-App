package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A single Google Form response pulled into the app. Unique per (form, googleResponseId) to dedupe re-syncs. */
@Document(collection = "formResponses")
@CompoundIndex(name = "form_response_unique", def = "{'mentorFormId': 1, 'googleResponseId': 1}", unique = true)
public class FormResponseRecord {

    @Id
    private String id;
    private String mentorFormId;
    private String googleResponseId;
    private String respondentEmail;
    private List<Answer> answers = new ArrayList<>();
    private Instant submittedAt;
    private Instant syncedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMentorFormId() { return mentorFormId; }
    public void setMentorFormId(String mentorFormId) { this.mentorFormId = mentorFormId; }

    public String getGoogleResponseId() { return googleResponseId; }
    public void setGoogleResponseId(String googleResponseId) { this.googleResponseId = googleResponseId; }

    public String getRespondentEmail() { return respondentEmail; }
    public void setRespondentEmail(String respondentEmail) { this.respondentEmail = respondentEmail; }

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant syncedAt) { this.syncedAt = syncedAt; }
}
