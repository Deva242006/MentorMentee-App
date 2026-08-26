package com.example.MentorMentee.web;

import com.example.MentorMentee.dto.AuthDtos.LoginRequest;
import com.example.MentorMentee.dto.AuthDtos.LoginResponse;
import com.example.MentorMentee.dto.UserDtos.UserView;
import com.example.MentorMentee.model.User;
import com.example.MentorMentee.repository.UserRepository;
import com.example.MentorMentee.security.AuthUser;
import com.example.MentorMentee.security.JwtService;
import com.example.MentorMentee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UserService userService;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, UserService userService) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (!u.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }
        return new LoginResponse(jwt.issue(u), userService.toView(u));
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal AuthUser me) {
        return userService.viewById(me.id());
    }
}
