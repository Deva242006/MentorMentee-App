package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.TrackingDtos.AssessmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentView;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentView;
import com.example.MentorMentee.dto.TrackingDtos.GradeRequest;
import com.example.MentorMentee.dto.TrackingDtos.TaskRequest;
import com.example.MentorMentee.dto.TrackingDtos.TaskStatusRequest;
import com.example.MentorMentee.dto.TrackingDtos.TaskView;
import com.example.MentorMentee.dto.UserDtos.MenteeSummary;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.AssessmentService;
import com.example.MentorMentee.service.AssignmentService;
import com.example.MentorMentee.service.TaskService;
import com.example.MentorMentee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Mentor-facing endpoints for their assigned mentees. Guarded by {@code /api/mentor/**} → MENTOR. */
@RestController
@RequestMapping("/api/mentor")
public class MentorController {

    private final UserService users;
    private final AssessmentService assessments;
    private final AssignmentService assignments;
    private final TaskService tasks;

    public MentorController(UserService users, AssessmentService assessments,
                            AssignmentService assignments, TaskService tasks) {
        this.users = users;
        this.assessments = assessments;
        this.assignments = assignments;
        this.tasks = tasks;
    }

    // --- mentees ---

    @GetMapping("/mentees")
    public List<MenteeSummary> mentees(@AuthenticationPrincipal AuthUser me) {
        return users.menteeSummaries(me.id());
    }

    @GetMapping("/mentees/{menteeId}")
    public MenteeSummary mentee(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId) {
        return users.summaryFor(users.requireMentee(me.id(), menteeId));
    }

    // --- assessments ---

    @GetMapping("/mentees/{menteeId}/assessments")
    public List<AssessmentView> assessments(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId) {
        users.requireMentee(me.id(), menteeId);
        return assessments.listForMentee(menteeId);
    }

    @PostMapping("/mentees/{menteeId}/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentView addAssessment(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId,
                                        @Valid @RequestBody AssessmentRequest req) {
        return assessments.create(users.requireMentee(me.id(), menteeId), me.id(), req);
    }

    @PutMapping("/assessments/{id}")
    public AssessmentView updateAssessment(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                           @Valid @RequestBody AssessmentRequest req) {
        return assessments.update(me.id(), id, req);
    }

    @DeleteMapping("/assessments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssessment(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        assessments.delete(me.id(), id);
    }

    // --- assignments ---

    @GetMapping("/mentees/{menteeId}/assignments")
    public List<AssignmentView> assignments(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId) {
        users.requireMentee(me.id(), menteeId);
        return assignments.listForMentee(menteeId);
    }

    @PostMapping("/mentees/{menteeId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentView addAssignment(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId,
                                        @Valid @RequestBody AssignmentRequest req) {
        return assignments.create(users.requireMentee(me.id(), menteeId), me.id(), req);
    }

    @PutMapping("/assignments/{id}/grade")
    public AssignmentView gradeAssignment(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                          @RequestBody GradeRequest req) {
        return assignments.grade(me.id(), id, req);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        assignments.delete(me.id(), id);
    }

    // --- tasks ---

    @GetMapping("/mentees/{menteeId}/tasks")
    public List<TaskView> tasks(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId) {
        users.requireMentee(me.id(), menteeId);
        return tasks.listForMentee(menteeId);
    }

    @PostMapping("/mentees/{menteeId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskView addTask(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId,
                            @Valid @RequestBody TaskRequest req) {
        return tasks.create(users.requireMentee(me.id(), menteeId), me.id(), req);
    }

    @PutMapping("/tasks/{id}")
    public TaskView updateTask(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                               @Valid @RequestBody TaskRequest req) {
        return tasks.update(me.id(), id, req);
    }

    @PutMapping("/tasks/{id}/status")
    public TaskView setTaskStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                  @Valid @RequestBody TaskStatusRequest req) {
        return tasks.setStatusAsMentor(me.id(), id, req.status());
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        tasks.delete(me.id(), id);
    }
}
