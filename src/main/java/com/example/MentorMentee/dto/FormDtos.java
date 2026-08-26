package com.example.MentorMentee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

/** Payloads for the Google Forms feature: form building, form/response views and Google link status. */
public final class FormDtos {

    /** One question in a form-builder request or view. */
    public record FormQuestionDto(
            @NotBlank String type,      // TEXT, PARAGRAPH, MULTIPLE_CHOICE, CHECKBOX, DROPDOWN, SCALE
            @NotBlank String title,
            boolean required,
            List<String> options) {}

    public record CreateFormRequest(
            @NotBlank String title,
            String description,
            @NotEmpty(message = "Add at least one question") List<@Valid FormQuestionDto> questions,
            List<String> assignedMenteeIds) {}

    public record FormView(
            String id,
            String googleFormId,
            String title,
            String description,
            String responderUri,
            String editUri,
            List<FormQuestionDto> questions,
            List<String> assignedMenteeIds,
            int responseCount,
            Instant lastSyncedAt,
            Instant createdAt) {}

    public record AnswerView(String questionTitle, List<String> values) {}

    public record FormResponseView(
            String id,
            String respondentEmail,
            Instant submittedAt,
            List<AnswerView> answers) {}

    public record FormDetailView(FormView form, List<FormResponseView> responses) {}

    /** What a mentee sees for a form assigned to them. */
    public record AssignedFormView(String id, String title, String description, String responderUri) {}

    public record GoogleStatusView(boolean enabled, boolean connected, String email) {}

    public record ConnectResponse(String authUrl) {}

    private FormDtos() {}
}
