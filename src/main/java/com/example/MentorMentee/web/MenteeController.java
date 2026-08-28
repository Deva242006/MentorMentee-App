package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.TrackingDtos.MenteeAssessmentView;
import com.example.MentorMentee.dto.TrackingDtos.MenteeAssignmentView;
import com.example.MentorMentee.dto.TrackingDtos.MenteeTaskView;
import com.example.MentorMentee.dto.TrackingDtos.TaskStatusRequest;
import com.example.MentorMentee.dto.UserDtos.UserView;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.AssessmentService;
import com.example.MentorMentee.service.AssignmentService;
import com.example.MentorMentee.service.TaskService;
import com.example.MentorMentee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mentee-facing endpoints: the mentee sees every item their mentor broadcast (with their own status joined in)
 * and updates their own task status. Guarded by {@code /api/mentee/**} → MENTEE.
 */
@RestController
@RequestMapping("/api/mentee")
public class MenteeController {

    private final UserService users;
    private final AssessmentService assessments;
    private final AssignmentService assignments;
    private final TaskService tasks;

    public MenteeController(UserService users, AssessmentService assessments,
                            AssignmentService assignments, TaskService tasks) {
        this.users = users;
        this.assessments = assessments;
        this.assignments = assignments;
        this.tasks = tasks;
    }

    @GetMapping("/profile")
    public UserView profile(@AuthenticationPrincipal AuthUser me) {
        return users.viewById(me.id());
    }

    @GetMapping("/assessments")
    public List<MenteeAssessmentView> assessments(@AuthenticationPrincipal AuthUser me) {
        return assessments.listForMentee(users.getUser(me.id()));
    }

    @GetMapping("/assignments")
    public List<MenteeAssignmentView> assignments(@AuthenticationPrincipal AuthUser me) {
        return assignments.listForMentee(users.getUser(me.id()));
    }

    @GetMapping("/tasks")
    public List<MenteeTaskView> tasks(@AuthenticationPrincipal AuthUser me) {
        return tasks.listForMentee(users.getUser(me.id()));
    }

    @PutMapping("/tasks/{id}/status")
    public MenteeTaskView setTaskStatus(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                        @Valid @RequestBody TaskStatusRequest req) {
        return tasks.setStatusAsMentee(users.getUser(me.id()), id, req.status());
    }
}
