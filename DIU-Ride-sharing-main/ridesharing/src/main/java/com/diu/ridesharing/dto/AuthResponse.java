package com.diu.ridesharing.dto;

import com.diu.ridesharing.entity.User;
import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String email;
    private String role;
    private String name;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.email = user.getEmail();
        this.role = user.getRole() != null ? user.getRole().name() : null;
        this.name = user.getName();
    }
}