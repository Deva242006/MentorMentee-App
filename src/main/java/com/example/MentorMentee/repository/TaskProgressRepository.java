package com.example.MentorMentee.repository;

import com.example.MentorMentee.model.TaskProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskProgressRepository extends MongoRepository<TaskProgress, String> {
    List<TaskProgress> findByTaskId(String taskId);
    Optional<TaskProgress> findByTaskIdAndMenteeId(String taskId, String menteeId);
    List<TaskProgress> findByTaskIdIn(Collection<String> taskIds);
    List<TaskProgress> findByMenteeId(String menteeId);
    void deleteByTaskId(String taskId);
    void deleteByMenteeId(String menteeId);
}
