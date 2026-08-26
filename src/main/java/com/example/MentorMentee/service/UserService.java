package com.example.MentorMentee.service;

import com.example.MentorMentee.dto.UserDtos.AdminStats;
import com.example.MentorMentee.dto.UserDtos.CreateUserRequest;
import com.example.MentorMentee.dto.UserDtos.MenteeSummary;
import com.example.MentorMentee.dto.UserDtos.UpdateUserRequest;
import com.example.MentorMentee.dto.UserDtos.UserView;
import com.example.MentorMentee.model.AssignmentStatus;
import com.example.MentorMentee.model.Role;
import com.example.MentorMentee.model.TaskStatus;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.AssessmentRepository;
import com.example.MentorMentee.repository.AssignmentRepository;
import com.example.MentorMentee.repository.DocumentMetaRepository;
import com.example.MentorMentee.repository.GoogleAccountRepository;
import com.example.MentorMentee.repository.MentorFormRepository;
import com.example.MentorMentee.repository.TaskRepository;
import com.example.MentorMentee.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** User provisioning (admin), view mapping, ownership checks and dashboard counts. */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AssessmentRepository assessments;
    private final AssignmentRepository assignments;
    private final TaskRepository tasks;
    private final DocumentMetaRepository documents;
    private final MentorFormRepository forms;
    private final GoogleAccountRepository googleAccounts;
    private final DocumentService documentService;

    public UserService(UserRepository users, PasswordEncoder encoder,
                       AssessmentRepository assessments, AssignmentRepository assignments,
                       TaskRepository tasks, DocumentMetaRepository documents,
                       MentorFormRepository forms, GoogleAccountRepository googleAccounts,
                       DocumentService documentService) {
        this.users = users;
        this.encoder = encoder;
        this.assessments = assessments;
        this.assignments = assignments;
        this.tasks = tasks;
        this.documents = documents;
        this.forms = forms;
        this.googleAccounts = googleAccounts;
        this.documentService = documentService;
    }

    // --- lookups & views ---

    public User getUser(String id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserView viewById(String id) {
        return toView(getUser(id));
    }

    public UserView toView(User u) {
        String mentorName = null;
        if (u.getMentorId() != null) {
            mentorName = users.findById(u.getMentorId()).map(User::getFullName).orElse(null);
        }
        return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name(), u.isActive(),
                u.getMentorId(), mentorName, u.getPhone(), u.getProgram(), u.getYear(),
                u.isGoogleConnected(), u.getCreatedAt());
    }

    /** Returns the mentee if it exists and is assigned to {@code mentorId}, else 404/403. */
    public User requireMentee(String mentorId, String menteeId) {
        User mentee = getUser(menteeId);
        if (mentee.getRole() != Role.MENTEE || !mentorId.equals(mentee.getMentorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your mentee");
        }
        return mentee;
    }

    // --- admin CRUD ---

    public List<UserView> list(Role role) {
        List<User> found = role == null ? users.findAll() : users.findByRole(role);
        return found.stream().map(this::toView).toList();
    }

    public List<UserView> mentors() {
        return users.findByRole(Role.MENTOR).stream().map(this::toView).toList();
    }

    public UserView create(CreateUserRequest req) {
        if (users.existsByEmail(req.email().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        User u = new User();
        u.setFullName(req.fullName());
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(req.role());
        u.setActive(true);
        u.setPhone(req.phone());
        if (req.role() == Role.MENTEE) {
            u.setProgram(req.program());
            u.setYear(req.year());
            u.setMentorId(validatedMentorId(req.mentorId()));
        }
        return toView(users.save(u));
    }

    public UserView update(String id, UpdateUserRequest req) {
        User u = getUser(id);
        u.setFullName(req.fullName());
        if (req.active() != null) {
            u.setActive(req.active());
        }
        u.setPhone(req.phone());
        if (u.getRole() == Role.MENTEE) {
            u.setProgram(req.program());
            u.setYear(req.year());
            u.setMentorId(validatedMentorId(req.mentorId()));
        }
        return toView(users.save(u));
    }

    public UserView assignMentor(String menteeId, String mentorId) {
        User mentee = getUser(menteeId);
        if (mentee.getRole() != Role.MENTEE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only mentees can be assigned a mentor");
        }
        mentee.setMentorId(validatedMentorId(mentorId));
        return toView(users.save(mentee));
    }

    public void delete(String id, String currentUserId) {
        if (id.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account");
        }
        User u = getUser(id);
        switch (u.getRole()) {
            case MENTEE -> {
                assessments.deleteByMenteeId(id);
                assignments.deleteByMenteeId(id);
                tasks.deleteByMenteeId(id);
                documentService.deleteAllForMentee(id);
            }
            case MENTOR -> {
                users.findByMentorId(id).forEach(m -> {
                    m.setMentorId(null);
                    users.save(m);
                });
                forms.findByMentorIdOrderByCreatedAtDesc(id).forEach(forms::delete);
                googleAccounts.deleteByMentorId(id);
            }
            case ADMIN -> {
                if (users.countByRole(Role.ADMIN) <= 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the last admin");
                }
            }
        }
        users.delete(u);
    }

    public AdminStats stats() {
        return new AdminStats(
                users.countByRole(Role.MENTOR),
                users.countByRole(Role.MENTEE),
                users.countByRole(Role.ADMIN),
                forms.count());
    }

    // --- mentor dashboard ---

    public List<MenteeSummary> menteeSummaries(String mentorId) {
        return users.findByMentorId(mentorId).stream()
                .filter(u -> u.getRole() == Role.MENTEE)
                .map(this::summaryFor)
                .toList();
    }

    public MenteeSummary summaryFor(User mentee) {
        return new MenteeSummary(
                toView(mentee),
                assessments.countByMenteeId(mentee.getId()),
                assignments.countByMenteeIdAndStatusNot(mentee.getId(), AssignmentStatus.GRADED),
                tasks.countByMenteeIdAndStatusNot(mentee.getId(), TaskStatus.DONE),
                documents.countByMenteeId(mentee.getId()));
    }

    private String validatedMentorId(String mentorId) {
        if (mentorId == null || mentorId.isBlank()) {
            return null;
        }
        User mentor = users.findById(mentorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mentor not found"));
        if (mentor.getRole() != Role.MENTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user is not a mentor");
        }
        return mentor.getId();
    }
}
