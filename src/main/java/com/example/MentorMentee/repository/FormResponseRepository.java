package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.FormResponseRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FormResponseRepository extends MongoRepository<FormResponseRecord, String> {
    List<FormResponseRecord> findByMentorFormIdOrderBySubmittedAtDesc(String mentorFormId);
    boolean existsByMentorFormIdAndGoogleResponseId(String mentorFormId, String googleResponseId);
    long countByMentorFormId(String mentorFormId);
    void deleteByMentorFormId(String mentorFormId);
}
