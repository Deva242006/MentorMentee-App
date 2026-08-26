package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.Task;
import com.example.MentorMentee.model.TaskStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByMenteeIdOrderByCreatedAtDesc(String menteeId);
    long countByMenteeIdAndStatusNot(String menteeId, TaskStatus status);
    void deleteByMenteeId(String menteeId);
}
