package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.FormDtos.AnswerView;
import com.example.MentorMentee.dto.FormDtos.AssignedFormView;
import com.example.MentorMentee.dto.FormDtos.CreateFormRequest;
import com.example.MentorMentee.dto.FormDtos.FormDetailView;
import com.example.MentorMentee.dto.FormDtos.FormQuestionDto;
import com.example.MentorMentee.dto.FormDtos.FormResponseView;
import com.example.MentorMentee.dto.FormDtos.FormView;
import com.example.MentorMentee.model.Answer;
import com.example.MentorMentee.model.FormQuestion;
import com.example.MentorMentee.model.FormResponseRecord;
import com.example.MentorMentee.model.MentorForm;
import com.example.MentorMentee.repository.FormResponseRepository;
import com.example.MentorMentee.repository.MentorFormRepository;
import com.example.MentorMentee.service.GoogleFormsService.CreatedForm;
import com.example.MentorMentee.service.GoogleFormsService.RawResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ties {@link MentorForm} persistence to the Google Forms API: create, list, sync responses, delete. */
@Service
public class FormService {

    private final MentorFormRepository forms;
    private final FormResponseRepository responses;
    private final GoogleOAuthService oauth;
    private final GoogleFormsService google;
    private final UserService users;

    public FormService(MentorFormRepository forms, FormResponseRepository responses,
                       GoogleOAuthService oauth, GoogleFormsService google, UserService users) {
        this.forms = forms;
        this.responses = responses;
        this.oauth = oauth;
        this.google = google;
        this.users = users;
    }

    public List<FormView> list(String mentorId) {
        return forms.findByMentorIdOrderByCreatedAtDesc(mentorId).stream().map(this::toView).toList();
    }

    public FormView create(String mentorId, CreateFormRequest req) {
        List<String> assigned = validateAssigned(mentorId, req.assignedMenteeIds());
        String token = oauth.validAccessToken(mentorId);
        CreatedForm created = google.createForm(token, req.title(), req.description(), req.questions());

        MentorForm form = new MentorForm();
        form.setMentorId(mentorId);
        form.setGoogleFormId(created.formId());
        form.setTitle(req.title());
        form.setDescription(req.description());
        form.setResponderUri(created.responderUri());
        form.setEditUri("https://docs.google.com/forms/d/" + created.formId() + "/edit");
        form.setQuestions(created.questions());
        form.setAssignedMenteeIds(assigned);
        return toView(forms.save(form));
    }

    public FormDetailView detail(String mentorId, String formId) {
        MentorForm form = owned(mentorId, formId);
        List<FormResponseView> views = responses.findByMentorFormIdOrderBySubmittedAtDesc(formId)
                .stream().map(this::toResponseView).toList();
        return new FormDetailView(toView(form), views);
    }

    /** Pulls responses from Google and stores any not seen before. Returns the refreshed form view. */
    public FormView sync(String mentorId, String formId) {
        MentorForm form = owned(mentorId, formId);
        String token = oauth.validAccessToken(mentorId);
        List<RawResponse> raw = google.fetchResponses(token, form.getGoogleFormId());

        Map<String, String> titleByQuestionId = new LinkedHashMap<>();
        for (FormQuestion q : form.getQuestions()) {
            if (q.getQuestionId() != null) {
                titleByQuestionId.put(q.getQuestionId(), q.getTitle());
            }
        }

        for (RawResponse r : raw) {
            if (r.responseId() == null
                    || responses.existsByMentorFormIdAndGoogleResponseId(formId, r.responseId())) {
                continue;
            }
            FormResponseRecord rec = new FormResponseRecord();
            rec.setMentorFormId(formId);
            rec.setGoogleResponseId(r.responseId());
            rec.setRespondentEmail(r.respondentEmail());
            rec.setSubmittedAt(r.submittedAt());
            List<Answer> answers = new ArrayList<>();
            r.answersByQuestionId().forEach((qId, values) ->
                    answers.add(new Answer(qId, titleByQuestionId.getOrDefault(qId, qId), values)));
            rec.setAnswers(answers);
            responses.save(rec);
        }

        form.setResponseCount((int) responses.countByMentorFormId(formId));
        form.setLastSyncedAt(Instant.now());
        return toView(forms.save(form));
    }

    public void delete(String mentorId, String formId) {
        MentorForm form = owned(mentorId, formId);
        responses.deleteByMentorFormId(formId);
        forms.delete(form);
    }

    public List<AssignedFormView> assignedTo(String menteeId) {
        return forms.findByAssignedMenteeIdsContainingOrderByCreatedAtDesc(menteeId).stream()
                .map(f -> new AssignedFormView(f.getId(), f.getTitle(), f.getDescription(), f.getResponderUri()))
                .toList();
    }

    // --- helpers ---

    private MentorForm owned(String mentorId, String formId) {
        MentorForm form = forms.findById(formId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form not found"));
        if (!mentorId.equals(form.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your form");
        }
        return form;
    }

    private List<String> validateAssigned(String mentorId, List<String> menteeIds) {
        List<String> ok = new ArrayList<>();
        if (menteeIds != null) {
            for (String id : menteeIds) {
                users.requireMentee(mentorId, id); // throws if not this mentor's mentee
                ok.add(id);
            }
        }
        return ok;
    }

    private FormView toView(MentorForm f) {
        List<FormQuestionDto> qs = f.getQuestions().stream()
                .map(q -> new FormQuestionDto(q.getType(), q.getTitle(), q.isRequired(), q.getOptions()))
                .toList();
        return new FormView(f.getId(), f.getGoogleFormId(), f.getTitle(), f.getDescription(),
                f.getResponderUri(), f.getEditUri(), qs, f.getAssignedMenteeIds(),
                f.getResponseCount(), f.getLastSyncedAt(), f.getCreatedAt());
    }

    private FormResponseView toResponseView(FormResponseRecord r) {
        List<AnswerView> answers = r.getAnswers().stream()
                .map(a -> new AnswerView(a.getQuestionTitle(), a.getValues()))
                .toList();
        return new FormResponseView(r.getId(), r.getRespondentEmail(), r.getSubmittedAt(), answers);
    }
}
