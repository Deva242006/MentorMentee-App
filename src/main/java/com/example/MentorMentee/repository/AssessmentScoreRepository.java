package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.AssessmentScore;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AssessmentScoreRepository extends MongoRepository<AssessmentScore, String> {
    List<AssessmentScore> findByAssessmentId(String assessmentId);
    Optional<AssessmentScore> findByAssessmentIdAndMenteeId(String assessmentId, String menteeId);
    List<AssessmentScore> findByAssessmentIdIn(Collection<String> assessmentIds);
    List<AssessmentScore> findByMenteeId(String menteeId);
    void deleteByAssessmentId(String assessmentId);
    void deleteByMenteeId(String menteeId);
}
