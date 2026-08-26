package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.TrackingDtos.TaskRequest;
import com.example.MentorMentee.dto.TrackingDtos.TaskView;
import com.example.MentorMentee.model.Priority;
import com.example.MentorMentee.model.Task;
import com.example.MentorMentee.model.TaskStatus;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Action items a mentor sets for a mentee; the mentee moves them through their status. */
@Service
public class TaskService {

    private final TaskRepository repo;

    public TaskService(TaskRepository repo) {
        this.repo = repo;
    }

    public List<TaskView> listForMentee(String menteeId) {
        return repo.findByMenteeIdOrderByCreatedAtDesc(menteeId).stream().map(this::toView).toList();
    }

    public TaskView create(User mentee, String mentorId, TaskRequest req) {
        Task t = new Task();
        t.setMenteeId(mentee.getId());
        t.setMentorId(mentorId);
        apply(t, req);
        return toView(repo.save(t));
    }

    public TaskView update(String mentorId, String id, TaskRequest req) {
        Task t = ownedByMentor(mentorId, id);
        apply(t, req);
        return toView(repo.save(t));
    }

    public void delete(String mentorId, String id) {
        repo.delete(ownedByMentor(mentorId, id));
    }

    public TaskView setStatusAsMentor(String mentorId, String id, TaskStatus status) {
        Task t = ownedByMentor(mentorId, id);
        t.setStatus(status);
        return toView(repo.save(t));
    }

    public TaskView setStatusAsMentee(String menteeId, String id, TaskStatus status) {
        Task t = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!menteeId.equals(t.getMenteeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your task");
        }
        t.setStatus(status);
        return toView(repo.save(t));
    }

    private Task ownedByMentor(String mentorId, String id) {
        Task t = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!mentorId.equals(t.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your task");
        }
        return t;
    }

    private void apply(Task t, TaskRequest req) {
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setDueDate(req.dueDate());
        t.setPriority(req.priority() == null ? Priority.MEDIUM : req.priority());
    }

    private TaskView toView(Task t) {
        return new TaskView(t.getId(), t.getMenteeId(), t.getTitle(), t.getDescription(),
                t.getDueDate(), t.getPriority().name(), t.getStatus().name(), t.getCreatedAt());
    }
}
