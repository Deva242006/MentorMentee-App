package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.Assessment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssessmentRepository extends MongoRepository<Assessment, String> {
    List<Assessment> findByMenteeIdOrderByAssessedOnDesc(String menteeId);
    long countByMenteeId(String menteeId);
    void deleteByMenteeId(String menteeId);
}
