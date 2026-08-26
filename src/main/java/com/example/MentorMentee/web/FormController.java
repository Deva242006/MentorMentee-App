package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.FormDtos.AssignedFormView;
import com.example.MentorMentee.dto.FormDtos.CreateFormRequest;
import com.example.MentorMentee.dto.FormDtos.FormDetailView;
import com.example.MentorMentee.dto.FormDtos.FormView;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.FormService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Mentor form building + response viewing, and the mentee's view of forms assigned to them. */
@RestController
public class FormController {

    private final FormService forms;

    public FormController(FormService forms) {
        this.forms = forms;
    }

    // --- mentor ---

    @GetMapping("/api/mentor/forms")
    public List<FormView> list(@AuthenticationPrincipal AuthUser me) {
        return forms.list(me.id());
    }

    @PostMapping("/api/mentor/forms")
    @ResponseStatus(HttpStatus.CREATED)
    public FormView create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreateFormRequest req) {
        return forms.create(me.id(), req);
    }

    @GetMapping("/api/mentor/forms/{id}")
    public FormDetailView detail(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return forms.detail(me.id(), id);
    }

    @PostMapping("/api/mentor/forms/{id}/sync")
    public FormView sync(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return forms.sync(me.id(), id);
    }

    @DeleteMapping("/api/mentor/forms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        forms.delete(me.id(), id);
    }

    // --- mentee ---

    @GetMapping("/api/mentee/forms")
    public List<AssignedFormView> assigned(@AuthenticationPrincipal AuthUser me) {
        return forms.assignedTo(me.id());
    }
}
