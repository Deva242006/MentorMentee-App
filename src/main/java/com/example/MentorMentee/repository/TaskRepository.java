package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByMentorIdOrderByCreatedAtDesc(String mentorId);
    long countByMentorId(String mentorId);
    void deleteByMentorId(String mentorId);
}
