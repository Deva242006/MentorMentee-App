package com.example.MentorMentee.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One question in a Google Form. Used both as the create-request payload (questionId null)
 * and as the stored question map (questionId populated from Google after creation) so responses
 * can be displayed with their question titles.
 */
public class FormQuestion {

    /** TEXT, PARAGRAPH, MULTIPLE_CHOICE, CHECKBOX, DROPDOWN, SCALE */
    private String type;
    private String title;
    private boolean required;
    private List<String> options = new ArrayList<>();

    /** Assigned by Google after the form is created; null on the create request. */
    private String questionId;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
}
