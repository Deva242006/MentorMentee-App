package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.AssignmentSubmission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends MongoRepository<AssignmentSubmission, String> {
    List<AssignmentSubmission> findByAssignmentId(String assignmentId);
    Optional<AssignmentSubmission> findByAssignmentIdAndMenteeId(String assignmentId, String menteeId);
    List<AssignmentSubmission> findByAssignmentIdIn(Collection<String> assignmentIds);
    List<AssignmentSubmission> findByMenteeId(String menteeId);
    void deleteByAssignmentId(String assignmentId);
    void deleteByMenteeId(String menteeId);
}
