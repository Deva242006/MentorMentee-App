package com.example.MentorMentee.model;

import java.util.ArrayList;
import java.util.List;

/** One answer within a {@link FormResponseRecord}. {@code values} holds one or more selected/entered values. */
public class Answer {

    private String questionId;
    private String questionTitle;
    private List<String> values = new ArrayList<>();

    public Answer() { }

    public Answer(String questionId, String questionTitle, List<String> values) {
        this.questionId = questionId;
        this.questionTitle = questionTitle;
        this.values = values;
    }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getQuestionTitle() { return questionTitle; }
    public void setQuestionTitle(String questionTitle) { this.questionTitle = questionTitle; }

    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }
}
