package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.Assignment;
import com.example.MentorMentee.model.AssignmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {
    List<Assignment> findByMenteeIdOrderByCreatedAtDesc(String menteeId);
    long countByMenteeIdAndStatusNot(String menteeId, AssignmentStatus status);
    void deleteByMenteeId(String menteeId);
}
