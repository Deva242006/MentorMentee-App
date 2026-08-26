package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A Google Form created by a mentor through the app. Responses are synced into {@link FormResponseRecord}. */
@Document(collection = "mentorForms")
public class MentorForm {

    @Id
    private String id;
    private String mentorId;

    private String googleFormId;
    private String title;
    private String description;
    private String responderUri;    // public link mentees open to answer
    private String editUri;         // Google Forms editor link for the mentor

    private List<FormQuestion> questions = new ArrayList<>();
    private List<String> assignedMenteeIds = new ArrayList<>();

    private Instant createdAt = Instant.now();
    private Instant lastSyncedAt;
    private int responseCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }

    public String getGoogleFormId() { return googleFormId; }
    public void setGoogleFormId(String googleFormId) { this.googleFormId = googleFormId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResponderUri() { return responderUri; }
    public void setResponderUri(String responderUri) { this.responderUri = responderUri; }

    public String getEditUri() { return editUri; }
    public void setEditUri(String editUri) { this.editUri = editUri; }

    public List<FormQuestion> getQuestions() { return questions; }
    public void setQuestions(List<FormQuestion> questions) { this.questions = questions; }

    public List<String> getAssignedMenteeIds() { return assignedMenteeIds; }
    public void setAssignedMenteeIds(List<String> assignedMenteeIds) { this.assignedMenteeIds = assignedMenteeIds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public int getResponseCount() { return responseCount; }
    public void setResponseCount(int responseCount) { this.responseCount = responseCount; }
}
