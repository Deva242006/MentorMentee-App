package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.UserDtos.AdminStats;
import com.example.MentorMentee.dto.UserDtos.AssignMentorRequest;
import com.example.MentorMentee.dto.UserDtos.CreateUserRequest;
import com.example.MentorMentee.dto.UserDtos.UpdateUserRequest;
import com.example.MentorMentee.dto.UserDtos.UserView;
import com.example.MentorMentee.model.Role;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only user provisioning and dashboard stats. Guarded by {@code /api/admin/**} → ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final UserService users;

    public AdminUserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/users")
    public List<UserView> list(@RequestParam(required = false) Role role) {
        return users.list(role);
    }

    @GetMapping("/users/{id}")
    public UserView get(@PathVariable String id) {
        return users.viewById(id);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(@Valid @RequestBody CreateUserRequest req) {
        return users.create(req);
    }

    @PutMapping("/users/{id}")
    public UserView update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest req) {
        return users.update(id, req);
    }

    @PutMapping("/users/{id}/mentor")
    public UserView assignMentor(@PathVariable String id, @RequestBody AssignMentorRequest req) {
        return users.assignMentor(id, req.mentorId());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, @AuthenticationPrincipal AuthUser me) {
        users.delete(id, me.id());
    }

    @GetMapping("/mentors")
    public List<UserView> mentors() {
        return users.mentors();
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return users.stats();
    }
}
