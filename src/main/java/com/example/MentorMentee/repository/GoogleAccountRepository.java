package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.GoogleAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GoogleAccountRepository extends MongoRepository<GoogleAccount, String> {
    Optional<GoogleAccount> findByMentorId(String mentorId);
    void deleteByMentorId(String mentorId);
}
