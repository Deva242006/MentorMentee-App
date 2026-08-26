package com.example.MentorMentee.security;

import com.example.MentorMentee.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads users by email for Spring Security. Defining this bean also disables Spring Boot's
 * auto-generated default user/password.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var u = users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .authorities("ROLE_" + u.getRole().name())
                .disabled(!u.isActive())
                .build();
    }
}
