package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {
    List<Assignment> findByMentorIdOrderByCreatedAtDesc(String mentorId);
    long countByMentorId(String mentorId);
    void deleteByMentorId(String mentorId);
}
