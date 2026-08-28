package com.example.MentorMentee.config;

import com.example.MentorMentee.model.Role;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds the admin account (and optionally a demo mentor + mentee) on first startup. */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;
    private final boolean seedDemo;

    public DataInitializer(UserRepository users,
                           PasswordEncoder encoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword,
                           @Value("${app.admin.name}") String adminName,
                           @Value("${app.seed-demo-data:false}") boolean seedDemo) {
        this.users = users;
        this.encoder = encoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
        this.seedDemo = seedDemo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByEmail(adminEmail).isEmpty()) {
            users.save(newUser(adminName, adminEmail, adminPassword, Role.ADMIN, null));
            log.info("Seeded admin account: {}", adminEmail);
        }

        if (seedDemo && users.findByEmail("mentor@mentortrack.local").isEmpty()) {
            User mentor = users.save(newUser("Demo Mentor", "mentor@mentortrack.local", "mentor123", Role.MENTOR, null));
            seedMentee(mentor, "Demo Mentee", "mentee@mentortrack.local", "mentee123", "B.E. Computer Science", "2");
            seedMentee(mentor, "Bala Subramanian", "bala@mentortrack.local", "mentee123", "B.E. Computer Science", "2");
            seedMentee(mentor, "Priya Raman", "priya@mentortrack.local", "mentee123", "B.E. Information Technology", "3");
            log.info("Seeded demo mentor (mentor@mentortrack.local / mentor123) and 3 mentees (mentee@/bala@/priya@mentortrack.local / mentee123)");
        }
    }

    private void seedMentee(User mentor, String name, String email, String rawPassword, String program, String year) {
        User mentee = newUser(name, email, rawPassword, Role.MENTEE, mentor.getId());
        mentee.setProgram(program);
        mentee.setYear(year);
        users.save(mentee);
    }

    private User newUser(String name, String email, String rawPassword, Role role, String mentorId) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(role);
        u.setActive(true);
        u.setMentorId(mentorId);
        return u;
    }
}
