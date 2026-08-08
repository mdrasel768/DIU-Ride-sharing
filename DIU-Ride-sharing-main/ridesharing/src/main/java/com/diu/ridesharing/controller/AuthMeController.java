package com.diu.ridesharing.controller;

import com.diu.ridesharing.entity.User;
import com.diu.ridesharing.repository.UserRepository;
import com.diu.ridesharing.security.AdminEmailRoleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthMeController {

    private final UserRepository userRepository;
    private final AdminEmailRoleResolver adminEmailRoleResolver;

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String email = auth.getName();

        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        String role = (u.getRole() != null) ? u.getRole().name() : "";

        if (adminEmailRoleResolver.isAdminEmail(email)) {
            role = "ADMIN";
        }

        return Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "role", role,
                "name", u.getName()
        );
    }
}