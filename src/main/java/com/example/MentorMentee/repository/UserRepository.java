package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.Role;
import com.example.MentorMentee.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    List<User> findByMentorId(String mentorId);
    long countByMentorId(String mentorId);
}
