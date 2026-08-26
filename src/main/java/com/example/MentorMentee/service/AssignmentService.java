package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.TrackingDtos.AssignmentRequest;
import com.example.MentorMentee.dto.TrackingDtos.AssignmentView;
import com.example.MentorMentee.dto.TrackingDtos.GradeRequest;
import com.example.MentorMentee.model.Assignment;
import com.example.MentorMentee.model.AssignmentStatus;
import com.example.MentorMentee.model.DocumentMeta;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.AssignmentRepository;
import com.example.MentorMentee.repository.DocumentMetaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/** Assignments set by a mentor, submitted (as a document) by the mentee, then graded. */
@Service
public class AssignmentService {

    private final AssignmentRepository repo;
    private final DocumentMetaRepository documents;

    public AssignmentService(AssignmentRepository repo, DocumentMetaRepository documents) {
        this.repo = repo;
        this.documents = documents;
    }

    public List<AssignmentView> listForMentee(String menteeId) {
        return repo.findByMenteeIdOrderByCreatedAtDesc(menteeId).stream().map(this::toView).toList();
    }

    public AssignmentView create(User mentee, String mentorId, AssignmentRequest req) {
        Assignment a = new Assignment();
        a.setMenteeId(mentee.getId());
        a.setMentorId(mentorId);
        a.setTitle(req.title());
        a.setDescription(req.description());
        a.setDueDate(req.dueDate());
        a.setStatus(AssignmentStatus.PENDING);
        return toView(repo.save(a));
    }

    public AssignmentView grade(String mentorId, String id, GradeRequest req) {
        Assignment a = ownedByMentor(mentorId, id);
        a.setGrade(req.grade());
        a.setFeedback(req.feedback());
        a.setStatus(AssignmentStatus.GRADED);
        return toView(repo.save(a));
    }

    public void delete(String mentorId, String id) {
        repo.delete(ownedByMentor(mentorId, id));
    }

    /** Called after a mentee uploads their submission document. */
    public AssignmentView submit(String menteeId, String id, DocumentMeta submission) {
        Assignment a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!menteeId.equals(a.getMenteeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your assignment");
        }
        a.setSubmissionDocId(submission.getId());
        a.setSubmittedAt(Instant.now());
        if (a.getStatus() != AssignmentStatus.GRADED) {
            a.setStatus(AssignmentStatus.SUBMITTED);
        }
        return toView(repo.save(a));
    }

    public Assignment getForMentee(String menteeId, String id) {
        Assignment a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!menteeId.equals(a.getMenteeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your assignment");
        }
        return a;
    }

    private Assignment ownedByMentor(String mentorId, String id) {
        Assignment a = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!mentorId.equals(a.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your assignment");
        }
        return a;
    }

    private AssignmentView toView(Assignment a) {
        String submissionName = a.getSubmissionDocId() == null ? null
                : documents.findById(a.getSubmissionDocId()).map(DocumentMeta::getOriginalName).orElse(null);
        return new AssignmentView(a.getId(), a.getMenteeId(), a.getTitle(), a.getDescription(),
                a.getDueDate(), a.getStatus().name(), a.getSubmissionDocId(), submissionName,
                a.getGrade(), a.getFeedback(), a.getCreatedAt(), a.getSubmittedAt());
    }
}
