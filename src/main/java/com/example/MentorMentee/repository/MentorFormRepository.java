package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.MentorForm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MentorFormRepository extends MongoRepository<MentorForm, String> {
    List<MentorForm> findByMentorIdOrderByCreatedAtDesc(String mentorId);
    List<MentorForm> findByAssignedMenteeIdsContainingOrderByCreatedAtDesc(String menteeId);
}
