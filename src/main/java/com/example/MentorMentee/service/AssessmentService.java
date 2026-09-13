package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.TrackingDtos.AssessmentProgress;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentProgressRow;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentSummary;
import com.example.MentorMentee.dto.TrackingDtos.MenteeAssessmentView;
import com.example.MentorMentee.dto.TrackingDtos.ScoreRequest;
import com.example.MentorMentee.model.Assessment;
import com.example.MentorMentee.model.AssessmentScore;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.AssessmentRepository;
import com.example.MentorMentee.repository.AssessmentScoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Assessments a mentor creates once for their whole cohort. After the deadline the mentor records each mentee's
 * score as an {@link AssessmentScore}; the mentor reads scored/pending counts and the class average.
 */
@Service
public class AssessmentService {

    private final AssessmentRepository repo;
    private final AssessmentScoreRepository scores;
    private final UserService users;
    private final NotificationService notifications;

    public AssessmentService(AssessmentRepository repo, AssessmentScoreRepository scores, UserService users, NotificationService notifications) {
        this.repo = repo;
        this.scores = scores;
        this.users = users;
        this.notifications = notifications;
    }

    // --- mentor: create / update / list / progress / score / delete ---

    public AssessmentSummary create(String mentorId, AssessmentRequest req) {
        Assessment a = new Assessment();
        a.setMentorId(mentorId);
        apply(a, req);
        repo.save(a);
        return summaryOf(a, users.menteesOf(mentorId).size(), List.of());
    }

    public AssessmentSummary update(String mentorId, String id, AssessmentRequest req) {
        Assessment a = ownedByMentor(mentorId, id);
        apply(a, req);
        repo.save(a);
        return summaryOf(a, users.menteesOf(mentorId).size(), scores.findByAssessmentId(id));
    }

    public List<AssessmentSummary> listForMentor(String mentorId) {
        List<Assessment> defs = repo.findByMentorIdOrderByCreatedAtDesc(mentorId);
        int total = users.menteesOf(mentorId).size();
        Map<String, List<AssessmentScore>> byAssessment = scores
                .findByAssessmentIdIn(defs.stream().map(Assessment::getId).toList())
                .stream().collect(Collectors.groupingBy(AssessmentScore::getAssessmentId));
        return defs.stream()
                .map(d -> summaryOf(d, total, byAssessment.getOrDefault(d.getId(), List.of())))
                .toList();
    }

    public AssessmentProgress progressFor(String mentorId, String id) {
        Assessment def = ownedByMentor(mentorId, id);
        List<User> mentees = users.menteesOf(mentorId);
        List<AssessmentScore> rowsData = scores.findByAssessmentId(id);
        Map<String, AssessmentScore> byMentee = rowsData.stream()
                .collect(Collectors.toMap(AssessmentScore::getMenteeId, s -> s, (a, b) -> a));
        List<AssessmentProgressRow> rows = mentees.stream()
                .map(m -> row(m, byMentee.get(m.getId())))
                .toList();
        return new AssessmentProgress(summaryOf(def, mentees.size(), rowsData), rows);
    }

    public AssessmentProgressRow setScore(String mentorId, String id, String menteeId, ScoreRequest req) {
        ownedByMentor(mentorId, id);
        User mentee = users.requireMentee(mentorId, menteeId);
        AssessmentScore s = scores.findByAssessmentIdAndMenteeId(id, menteeId)
                .orElseGet(() -> newScore(id, menteeId));
        s.setScore(req.score());
        s.setRemarks(req.remarks());
        s.setScoredAt(Instant.now());
        scores.save(s);
        
        Assessment def = repo.findById(id).orElse(null);
        String title = def != null ? def.getTitle() : "an assessment";
        notifications.notify(menteeId, "Your score for '" + title + "' is available.", "ASSESSMENT", "/mentee?tab=assessments");
        
        return row(mentee, s);
    }

    public void delete(String mentorId, String id) {
        Assessment def = ownedByMentor(mentorId, id);
        scores.deleteByAssessmentId(id);
        repo.delete(def);
    }

    // --- mentee: list ---

    public List<MenteeAssessmentView> listForMentee(User mentee) {
        if (mentee.getMentorId() == null) return List.of();
        List<Assessment> defs = repo.findByMentorIdOrderByCreatedAtDesc(mentee.getMentorId());
        Map<String, AssessmentScore> mine = scores.findByMenteeId(mentee.getId()).stream()
                .collect(Collectors.toMap(AssessmentScore::getAssessmentId, s -> s, (a, b) -> a));
        return defs.stream().map(d -> menteeView(d, mine.get(d.getId()))).toList();
    }

    // --- helpers ---

    private Assessment ownedByMentor(String mentorId, String id) {
        Assessment a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        if (!mentorId.equals(a.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your assessment");
        }
        return a;
    }

    private AssessmentScore newScore(String assessmentId, String menteeId) {
        AssessmentScore s = new AssessmentScore();
        s.setAssessmentId(assessmentId);
        s.setMenteeId(menteeId);
        return s;
    }

    private void apply(Assessment a, AssessmentRequest req) {
        a.setTitle(req.title());
        a.setType(req.type());
        a.setMaxScore(req.maxScore());
        a.setDueDate(req.dueDate());
    }

    private AssessmentSummary summaryOf(Assessment d, int total, List<AssessmentScore> rows) {
        long scored = rows.stream().filter(s -> s.getScore() != null).count();
        long pending = Math.max(0, total - scored);
        OptionalDouble avg = rows.stream().filter(s -> s.getScore() != null)
                .mapToDouble(AssessmentScore::getScore).average();
        return new AssessmentSummary(d.getId(), d.getTitle(), d.getType(), d.getMaxScore(), d.getDueDate(),
                d.getCreatedAt(), total, scored, pending, avg.isPresent() ? avg.getAsDouble() : null);
    }

    private AssessmentProgressRow row(User mentee, AssessmentScore s) {
        if (s == null) {
            return new AssessmentProgressRow(mentee.getId(), mentee.getFullName(), null, null, null);
        }
        return new AssessmentProgressRow(mentee.getId(), mentee.getFullName(),
                s.getScore(), s.getRemarks(), s.getScoredAt());
    }

    private MenteeAssessmentView menteeView(Assessment d, AssessmentScore s) {
        return new MenteeAssessmentView(d.getId(), d.getTitle(), d.getType(),
                s == null ? null : s.getScore(), d.getMaxScore(), d.getDueDate(),
                s == null ? null : s.getRemarks(),
                s == null ? null : s.getScoredAt());
    }
}
