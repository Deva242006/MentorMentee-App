package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.TrackingDtos.AssessmentProgress;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentProgressRow;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentSummary;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentProgress;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentProgressRow;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentSummary;
import com.example.MentorMentee.dto.TrackingDtos.GradeRequest;
import com.example.MentorMentee.dto.TrackingDtos.ScoreRequest;
import com.example.MentorMentee.dto.TrackingDtos.TaskProgressDetail;
import com.example.MentorMentee.dto.TrackingDtos.TaskRequest;
import com.example.MentorMentee.dto.TrackingDtos.TaskSummary;
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

/**
 * Mentor-facing endpoints. A mentor creates each assignment/assessment/task once for their whole cohort,
 * then reads aggregate progress and grades/scores individual mentees. Guarded by {@code /api/mentor/**} → MENTOR.
 */
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

    // --- mentees roster ---

    @GetMapping("/mentees")
    public List<MenteeSummary> mentees(@AuthenticationPrincipal AuthUser me) {
        return users.menteeSummaries(me.id());
    }

    @GetMapping("/mentees/{menteeId}")
    public MenteeSummary mentee(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId) {
        return users.summaryFor(users.requireMentee(me.id(), menteeId));
    }

    // --- assignments (created once for all mentees) ---

    @GetMapping("/assignments")
    public List<AssignmentSummary> assignments(@AuthenticationPrincipal AuthUser me) {
        return assignments.listForMentor(me.id());
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentSummary addAssignment(@AuthenticationPrincipal AuthUser me,
                                           @Valid @RequestBody AssignmentRequest req) {
        return assignments.create(me.id(), req);
    }

    @PutMapping("/assignments/{id}")
    public AssignmentSummary updateAssignment(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                              @Valid @RequestBody AssignmentRequest req) {
        return assignments.update(me.id(), id, req);
    }

    @GetMapping("/assignments/{id}/progress")
    public AssignmentProgress assignmentProgress(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return assignments.progressFor(me.id(), id);
    }

    @PutMapping("/assignments/{id}/mentees/{menteeId}/grade")
    public AssignmentProgressRow gradeAssignment(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                                 @PathVariable String menteeId, @RequestBody GradeRequest req) {
        return assignments.grade(me.id(), id, menteeId, req);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        assignments.delete(me.id(), id);
    }

    // --- assessments (mentor records each mentee's score) ---

    @GetMapping("/assessments")
    public List<AssessmentSummary> assessments(@AuthenticationPrincipal AuthUser me) {
        return assessments.listForMentor(me.id());
    }

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentSummary addAssessment(@AuthenticationPrincipal AuthUser me,
                                           @Valid @RequestBody AssessmentRequest req) {
        return assessments.create(me.id(), req);
    }

    @PutMapping("/assessments/{id}")
    public AssessmentSummary updateAssessment(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                              @Valid @RequestBody AssessmentRequest req) {
        return assessments.update(me.id(), id, req);
    }

    @GetMapping("/assessments/{id}/progress")
    public AssessmentProgress assessmentProgress(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return assessments.progressFor(me.id(), id);
    }

    @PutMapping("/assessments/{id}/mentees/{menteeId}/score")
    public AssessmentProgressRow scoreAssessment(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                                 @PathVariable String menteeId, @RequestBody ScoreRequest req) {
        return assessments.setScore(me.id(), id, menteeId, req);
    }

    @DeleteMapping("/assessments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssessment(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        assessments.delete(me.id(), id);
    }

    // --- tasks (mentees own their own status) ---

    @GetMapping("/tasks")
    public List<TaskSummary> tasks(@AuthenticationPrincipal AuthUser me) {
        return tasks.listForMentor(me.id());
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskSummary addTask(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody TaskRequest req) {
        return tasks.create(me.id(), req);
    }

    @PutMapping("/tasks/{id}")
    public TaskSummary updateTask(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                  @Valid @RequestBody TaskRequest req) {
        return tasks.update(me.id(), id, req);
    }

    @GetMapping("/tasks/{id}/progress")
    public TaskProgressDetail taskProgress(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return tasks.progressFor(me.id(), id);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        tasks.delete(me.id(), id);
    }
}
