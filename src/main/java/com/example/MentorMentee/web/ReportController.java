package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.TrackingDtos.AssessmentProgressRow;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentProgressRow;
import com.example.MentorMentee.dto.TrackingDtos.TaskProgressRow;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.AssessmentService;
import com.example.MentorMentee.service.AssignmentService;
import com.example.MentorMentee.service.TaskService;
import com.example.MentorMentee.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mentor")
public class ReportController {

    private final UserService users;
    private final AssessmentService assessments;
    private final AssignmentService assignments;
    private final TaskService tasks;

    public ReportController(UserService users, AssessmentService assessments,
                            AssignmentService assignments, TaskService tasks) {
        this.users = users;
        this.assessments = assessments;
        this.assignments = assignments;
        this.tasks = tasks;
    }

    @GetMapping("/mentees/{menteeId}/export")
    public ResponseEntity<String> exportMenteeReport(@AuthenticationPrincipal AuthUser me, @PathVariable String menteeId) {
        User mentee = users.requireMentee(me.id(), menteeId);

        StringBuilder csv = new StringBuilder();
        csv.append("MentorTrack Progress Report\n");
        csv.append("Mentee: ").append(mentee.getFullName()).append("\n");
        csv.append("Email: ").append(mentee.getEmail()).append("\n\n");

        csv.append("--- ASSESSMENTS ---\n");
        csv.append("Title,Type,Score,Max Score,Date Scored,Remarks\n");
        var menteeAssessments = assessments.listForMentee(mentee);
        for (var a : menteeAssessments) {
            csv.append(escape(a.title())).append(",")
               .append(escape(a.type())).append(",")
               .append(a.score() != null ? a.score() : "Pending").append(",")
               .append(a.maxScore() != null ? a.maxScore() : "").append(",")
               .append(a.scoredAt() != null ? a.scoredAt().toString() : "").append(",")
               .append(escape(a.remarks())).append("\n");
        }

        csv.append("\n--- ASSIGNMENTS ---\n");
        csv.append("Title,Due Date,Status,Grade,Submitted At,Feedback\n");
        var menteeAssignments = assignments.listForMentee(mentee);
        for (var a : menteeAssignments) {
            csv.append(escape(a.title())).append(",")
               .append(a.dueDate() != null ? a.dueDate().toString() : "").append(",")
               .append(a.status()).append(",")
               .append(a.grade() != null ? a.grade() : "").append(",")
               .append(a.submittedAt() != null ? a.submittedAt().toString() : "").append(",")
               .append(escape(a.feedback())).append("\n");
        }

        csv.append("\n--- TASKS ---\n");
        csv.append("Title,Priority,Due Date,Status\n");
        var menteeTasks = tasks.listForMentee(mentee);
        for (var t : menteeTasks) {
            csv.append(escape(t.title())).append(",")
               .append(t.priority()).append(",")
               .append(t.dueDate() != null ? t.dueDate().toString() : "").append(",")
               .append(t.status()).append("\n");
        }

        String filename = "report_" + mentee.getFullName().replaceAll("\\s+", "_") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
    
    private String escape(String value) {
        if (value == null) return "";
        String s = value.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
