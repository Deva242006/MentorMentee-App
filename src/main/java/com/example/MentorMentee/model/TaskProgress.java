package com.example.MentorMentee.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One mentee's progress on a shared {@link Task}. Created lazily when the mentee first moves the task off
 * TODO. No record for a (task, mentee) pair means the mentee is still TODO.
 */
@Document(collection = "task_progress")
public class TaskProgress {

    @Id
    private String id;
    private String taskId;
    private String menteeId;

    private TaskStatus status = TaskStatus.TODO;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getMenteeId() { return menteeId; }
    public void setMenteeId(String menteeId) { this.menteeId = menteeId; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
