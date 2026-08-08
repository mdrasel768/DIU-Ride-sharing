package com.diu.ridesharing.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminEmailRoleResolver {

    private final Set<String> adminEmails;

    public AdminEmailRoleResolver(@Value("${app.security.admin-emails:}") String emailsCsv) {
        this.adminEmails = Arrays.stream(emailsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.toLowerCase());
    }
}
