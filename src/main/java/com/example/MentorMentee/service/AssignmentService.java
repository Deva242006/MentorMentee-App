package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.TrackingDtos.AssignmentProgress;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentProgressRow;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentSummary;
import com.example.MentorMentee.dto.TrackingDtos.GradeRequest;
import com.example.MentorMentee.dto.TrackingDtos.MenteeAssignmentView;
import com.example.MentorMentee.model.Assignment;
import com.example.MentorMentee.model.AssignmentStatus;
import com.example.MentorMentee.model.AssignmentSubmission;
import com.example.MentorMentee.model.DocumentMeta;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.AssignmentRepository;
import com.example.MentorMentee.repository.AssignmentSubmissionRepository;
import com.example.MentorMentee.repository.DocumentMetaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Assignments a mentor creates once for their whole cohort. Each mentee's submission/grade is a separate
 * {@link AssignmentSubmission}; the mentor reads aggregate progress across their mentees.
 */
@Service
public class AssignmentService {

    private final AssignmentRepository repo;
    private final AssignmentSubmissionRepository submissions;
    private final DocumentMetaRepository documents;
    private final UserService users;
    private final NotificationService notifications;

    public AssignmentService(AssignmentRepository repo, AssignmentSubmissionRepository submissions,
                             DocumentMetaRepository documents, UserService users, NotificationService notifications) {
        this.repo = repo;
        this.submissions = submissions;
        this.documents = documents;
        this.users = users;
        this.notifications = notifications;
    }

    // --- mentor: create / update / list / progress / grade / delete ---

    public AssignmentSummary create(String mentorId, AssignmentRequest req) {
        Assignment a = new Assignment();
        a.setMentorId(mentorId);
        apply(a, req);
        repo.save(a);
        return summaryOf(a, users.menteesOf(mentorId).size(), List.of());
    }

    public AssignmentSummary update(String mentorId, String id, AssignmentRequest req) {
        Assignment a = ownedByMentor(mentorId, id);
        apply(a, req);
        repo.save(a);
        return summaryOf(a, users.menteesOf(mentorId).size(), submissions.findByAssignmentId(id));
    }

    public List<AssignmentSummary> listForMentor(String mentorId) {
        List<Assignment> defs = repo.findByMentorIdOrderByCreatedAtDesc(mentorId);
        int total = users.menteesOf(mentorId).size();
        Map<String, List<AssignmentSubmission>> byAssignment = submissions
                .findByAssignmentIdIn(defs.stream().map(Assignment::getId).toList())
                .stream().collect(Collectors.groupingBy(AssignmentSubmission::getAssignmentId));
        return defs.stream()
                .map(d -> summaryOf(d, total, byAssignment.getOrDefault(d.getId(), List.of())))
                .toList();
    }

    public AssignmentProgress progressFor(String mentorId, String id) {
        Assignment def = ownedByMentor(mentorId, id);
        List<User> mentees = users.menteesOf(mentorId);
        List<AssignmentSubmission> subs = submissions.findByAssignmentId(id);
        Map<String, AssignmentSubmission> byMentee = subs.stream()
                .collect(Collectors.toMap(AssignmentSubmission::getMenteeId, s -> s, (a, b) -> a));
        List<AssignmentProgressRow> rows = mentees.stream()
                .map(m -> row(m, byMentee.get(m.getId())))
                .toList();
        return new AssignmentProgress(summaryOf(def, mentees.size(), subs), rows);
    }

    public AssignmentProgressRow grade(String mentorId, String id, String menteeId, GradeRequest req) {
        ownedByMentor(mentorId, id);
        User mentee = users.requireMentee(mentorId, menteeId);
        AssignmentSubmission s = submissions.findByAssignmentIdAndMenteeId(id, menteeId)
                .orElseGet(() -> newSubmission(id, menteeId));
        s.setGrade(req.grade());
        s.setFeedback(req.feedback());
        s.setStatus(AssignmentStatus.GRADED);
        s.setGradedAt(Instant.now());
        submissions.save(s);
        
        Assignment def = repo.findById(id).orElse(null);
        String title = def != null ? def.getTitle() : "an assignment";
        notifications.notify(menteeId, "Your assignment '" + title + "' has been graded.", "ASSIGNMENT", "/mentee?tab=assignments");
        
        return row(mentee, s);
    }

    public void delete(String mentorId, String id) {
        Assignment def = ownedByMentor(mentorId, id);
        submissions.deleteByAssignmentId(id);
        repo.delete(def);
    }

    // --- mentee: list / submit ---

    public List<MenteeAssignmentView> listForMentee(User mentee) {
        if (mentee.getMentorId() == null) return List.of();
        List<Assignment> defs = repo.findByMentorIdOrderByCreatedAtDesc(mentee.getMentorId());
        Map<String, AssignmentSubmission> mine = submissions.findByMenteeId(mentee.getId()).stream()
                .collect(Collectors.toMap(AssignmentSubmission::getAssignmentId, s -> s, (a, b) -> a));
        return defs.stream().map(d -> menteeView(d, mine.get(d.getId()))).toList();
    }

    /** Validates the mentee may submit to this assignment (it belongs to their mentor) before storing bytes. */
    public void assertCanSubmit(User mentee, String assignmentId) {
        requireVisible(mentee, assignmentId);
    }

    public MenteeAssignmentView submit(User mentee, String assignmentId, DocumentMeta doc) {
        Assignment def = requireVisible(mentee, assignmentId);
        AssignmentSubmission s = submissions.findByAssignmentIdAndMenteeId(assignmentId, mentee.getId())
                .orElseGet(() -> newSubmission(assignmentId, mentee.getId()));
        s.setSubmissionDocId(doc.getId());
        s.setSubmittedAt(Instant.now());
        if (s.getStatus() != AssignmentStatus.GRADED) {
            s.setStatus(AssignmentStatus.SUBMITTED);
        }
        submissions.save(s);
        
        notifications.notify(def.getMentorId(), mentee.getFullName() + " submitted assignment '" + def.getTitle() + "'.", "ASSIGNMENT", "/mentor/mentees/" + mentee.getId() + "?tab=assignments");
        
        return menteeView(def, s);
    }

    // --- helpers ---

    private Assignment requireVisible(User mentee, String assignmentId) {
        Assignment def = repo.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (mentee.getMentorId() == null || !mentee.getMentorId().equals(def.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your assignment");
        }
        return def;
    }

    private Assignment ownedByMentor(String mentorId, String id) {
        Assignment a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!mentorId.equals(a.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your assignment");
        }
        return a;
    }

    private AssignmentSubmission newSubmission(String assignmentId, String menteeId) {
        AssignmentSubmission s = new AssignmentSubmission();
        s.setAssignmentId(assignmentId);
        s.setMenteeId(menteeId);
        s.setStatus(AssignmentStatus.PENDING);
        return s;
    }

    private void apply(Assignment a, AssignmentRequest req) {
        a.setTitle(req.title());
        a.setDescription(req.description());
        a.setDueDate(req.dueDate());
    }

    private AssignmentSummary summaryOf(Assignment d, int total, List<AssignmentSubmission> subs) {
        long submitted = subs.stream().filter(s -> s.getStatus() == AssignmentStatus.SUBMITTED).count();
        long graded = subs.stream().filter(s -> s.getStatus() == AssignmentStatus.GRADED).count();
        long pending = Math.max(0, total - submitted - graded);
        OptionalDouble avg = subs.stream().filter(s -> s.getGrade() != null)
                .mapToDouble(AssignmentSubmission::getGrade).average();
        return new AssignmentSummary(d.getId(), d.getTitle(), d.getDescription(), d.getDueDate(),
                d.getCreatedAt(), total, submitted, graded, pending,
                avg.isPresent() ? avg.getAsDouble() : null);
    }

    private AssignmentProgressRow row(User mentee, AssignmentSubmission s) {
        if (s == null) {
            return new AssignmentProgressRow(mentee.getId(), mentee.getFullName(),
                    AssignmentStatus.PENDING.name(), null, null, null, null, null);
        }
        return new AssignmentProgressRow(mentee.getId(), mentee.getFullName(), s.getStatus().name(),
                s.getSubmissionDocId(), nameOf(s.getSubmissionDocId()), s.getSubmittedAt(),
                s.getGrade(), s.getFeedback());
    }

    private MenteeAssignmentView menteeView(Assignment d, AssignmentSubmission s) {
        String status = s == null ? AssignmentStatus.PENDING.name() : s.getStatus().name();
        return new MenteeAssignmentView(d.getId(), d.getTitle(), d.getDescription(), d.getDueDate(), status,
                s == null ? null : s.getSubmissionDocId(),
                s == null ? null : nameOf(s.getSubmissionDocId()),
                s == null ? null : s.getGrade(),
                s == null ? null : s.getFeedback(),
                s == null ? null : s.getSubmittedAt());
    }

    private String nameOf(String docId) {
        return docId == null ? null
                : documents.findById(docId).map(DocumentMeta::getOriginalName).orElse(null);
    }
}
