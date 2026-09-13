package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.TrackingDtos.MenteeTaskView;
import com.example.MentorMentee.dto.TrackingDtos.TaskProgressDetail;
import com.example.MentorMentee.dto.TrackingDtos.TaskProgressRow;
import com.example.MentorMentee.dto.TrackingDtos.TaskRequest;
import com.example.MentorMentee.dto.TrackingDtos.TaskSummary;
import com.example.MentorMentee.model.Priority;
import com.example.MentorMentee.model.Task;
import com.example.MentorMentee.model.TaskProgress;
import com.example.MentorMentee.model.TaskStatus;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.TaskProgressRepository;
import com.example.MentorMentee.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tasks a mentor creates once for their whole cohort. Each mentee owns their own {@link TaskProgress}
 * (todo / in-progress / done); the mentor reads how many mentees are done.
 */
@Service
public class TaskService {

    private final TaskRepository repo;
    private final TaskProgressRepository progress;
    private final UserService users;
    private final NotificationService notifications;

    public TaskService(TaskRepository repo, TaskProgressRepository progress, UserService users, NotificationService notifications) {
        this.repo = repo;
        this.progress = progress;
        this.users = users;
        this.notifications = notifications;
    }

    // --- mentor: create / update / list / progress / delete ---

    public TaskSummary create(String mentorId, TaskRequest req) {
        Task t = new Task();
        t.setMentorId(mentorId);
        apply(t, req);
        repo.save(t);
        return summaryOf(t, users.menteesOf(mentorId).size(), List.of());
    }

    public TaskSummary update(String mentorId, String id, TaskRequest req) {
        Task t = ownedByMentor(mentorId, id);
        apply(t, req);
        repo.save(t);
        return summaryOf(t, users.menteesOf(mentorId).size(), progress.findByTaskId(id));
    }

    public List<TaskSummary> listForMentor(String mentorId) {
        List<Task> defs = repo.findByMentorIdOrderByCreatedAtDesc(mentorId);
        int total = users.menteesOf(mentorId).size();
        Map<String, List<TaskProgress>> byTask = progress
                .findByTaskIdIn(defs.stream().map(Task::getId).toList())
                .stream().collect(Collectors.groupingBy(TaskProgress::getTaskId));
        return defs.stream()
                .map(d -> summaryOf(d, total, byTask.getOrDefault(d.getId(), List.of())))
                .toList();
    }

    public TaskProgressDetail progressFor(String mentorId, String id) {
        Task def = ownedByMentor(mentorId, id);
        List<User> mentees = users.menteesOf(mentorId);
        List<TaskProgress> rowsData = progress.findByTaskId(id);
        Map<String, TaskProgress> byMentee = rowsData.stream()
                .collect(Collectors.toMap(TaskProgress::getMenteeId, p -> p, (a, b) -> a));
        List<TaskProgressRow> rows = mentees.stream()
                .map(m -> row(m, byMentee.get(m.getId())))
                .toList();
        return new TaskProgressDetail(summaryOf(def, mentees.size(), rowsData), rows);
    }

    public void delete(String mentorId, String id) {
        Task def = ownedByMentor(mentorId, id);
        progress.deleteByTaskId(id);
        repo.delete(def);
    }

    // --- mentee: list / set own status ---

    public List<MenteeTaskView> listForMentee(User mentee) {
        if (mentee.getMentorId() == null) return List.of();
        List<Task> defs = repo.findByMentorIdOrderByCreatedAtDesc(mentee.getMentorId());
        Map<String, TaskProgress> mine = progress.findByMenteeId(mentee.getId()).stream()
                .collect(Collectors.toMap(TaskProgress::getTaskId, p -> p, (a, b) -> a));
        return defs.stream().map(d -> menteeView(d, mine.get(d.getId()))).toList();
    }

    public MenteeTaskView setStatusAsMentee(User mentee, String taskId, TaskStatus status) {
        Task def = requireVisible(mentee, taskId);
        TaskProgress p = progress.findByTaskIdAndMenteeId(taskId, mentee.getId())
                .orElseGet(() -> newProgress(taskId, mentee.getId()));
        p.setStatus(status);
        p.setUpdatedAt(Instant.now());
        progress.save(p);
        
        if (status == TaskStatus.DONE) {
            notifications.notify(def.getMentorId(), mentee.getFullName() + " completed task '" + def.getTitle() + "'.", "TASK", "/mentor/mentees/" + mentee.getId() + "?tab=tasks");
        }
        
        return menteeView(def, p);
    }

    // --- helpers ---

    private Task requireVisible(User mentee, String taskId) {
        Task def = repo.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (mentee.getMentorId() == null || !mentee.getMentorId().equals(def.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your task");
        }
        return def;
    }

    private Task ownedByMentor(String mentorId, String id) {
        Task t = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!mentorId.equals(t.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your task");
        }
        return t;
    }

    private TaskProgress newProgress(String taskId, String menteeId) {
        TaskProgress p = new TaskProgress();
        p.setTaskId(taskId);
        p.setMenteeId(menteeId);
        p.setStatus(TaskStatus.TODO);
        return p;
    }

    private void apply(Task t, TaskRequest req) {
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setDueDate(req.dueDate());
        t.setPriority(req.priority() == null ? Priority.MEDIUM : req.priority());
    }

    private TaskSummary summaryOf(Task d, int total, List<TaskProgress> rows) {
        long done = rows.stream().filter(p -> p.getStatus() == TaskStatus.DONE).count();
        long inProgress = rows.stream().filter(p -> p.getStatus() == TaskStatus.IN_PROGRESS).count();
        long todo = Math.max(0, total - done - inProgress);
        return new TaskSummary(d.getId(), d.getTitle(), d.getDescription(), d.getDueDate(),
                d.getPriority() == null ? null : d.getPriority().name(), d.getCreatedAt(),
                total, done, inProgress, todo);
    }

    private TaskProgressRow row(User mentee, TaskProgress p) {
        String status = p == null ? TaskStatus.TODO.name() : p.getStatus().name();
        return new TaskProgressRow(mentee.getId(), mentee.getFullName(), status,
                p == null ? null : p.getUpdatedAt());
    }

    private MenteeTaskView menteeView(Task d, TaskProgress p) {
        String status = p == null ? TaskStatus.TODO.name() : p.getStatus().name();
        return new MenteeTaskView(d.getId(), d.getTitle(), d.getDescription(), d.getDueDate(),
                d.getPriority() == null ? null : d.getPriority().name(), status);
    }
}
