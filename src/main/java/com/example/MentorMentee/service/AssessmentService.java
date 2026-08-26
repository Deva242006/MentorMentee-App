package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.TrackingDtos.AssessmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssessmentView;
import com.example.MentorMentee.model.Assessment;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.AssessmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Assessments recorded by a mentor for a mentee. */
@Service
public class AssessmentService {

    private final AssessmentRepository repo;

    public AssessmentService(AssessmentRepository repo) {
        this.repo = repo;
    }

    public List<AssessmentView> listForMentee(String menteeId) {
        return repo.findByMenteeIdOrderByAssessedOnDesc(menteeId).stream().map(this::toView).toList();
    }

    public AssessmentView create(User mentee, String mentorId, AssessmentRequest req) {
        Assessment a = new Assessment();
        a.setMenteeId(mentee.getId());
        a.setMentorId(mentorId);
        apply(a, req);
        return toView(repo.save(a));
    }

    public AssessmentView update(String mentorId, String id, AssessmentRequest req) {
        Assessment a = ownedByMentor(mentorId, id);
        apply(a, req);
        return toView(repo.save(a));
    }

    public void delete(String mentorId, String id) {
        repo.delete(ownedByMentor(mentorId, id));
    }

    private Assessment ownedByMentor(String mentorId, String id) {
        Assessment a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        if (!mentorId.equals(a.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your assessment");
        }
        return a;
    }

    private void apply(Assessment a, AssessmentRequest req) {
        a.setTitle(req.title());
        a.setType(req.type());
        a.setScore(req.score());
        a.setMaxScore(req.maxScore());
        a.setAssessedOn(req.assessedOn());
        a.setRemarks(req.remarks());
    }

    private AssessmentView toView(Assessment a) {
        return new AssessmentView(a.getId(), a.getMenteeId(), a.getTitle(), a.getType(),
                a.getScore(), a.getMaxScore(), a.getAssessedOn(), a.getRemarks(), a.getCreatedAt());
    }
}
