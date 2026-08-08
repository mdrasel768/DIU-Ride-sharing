package com.diu.ridesharing.bootstrap;

import com.diu.ridesharing.entity.Admin;
import com.diu.ridesharing.entity.User;
import com.diu.ridesharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnsureAdminUserRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.admin.email}")
    private String adminEmail;
    @org.springframework.beans.factory.annotation.Value("${app.admin.password}")
    private String adminPassword;

    // Defaults (privacyMask=true হলে এগুলো enforce হবে)
    @org.springframework.beans.factory.annotation.Value("${app.admin.name:System Admin}")
    private String adminName;
    @org.springframework.beans.factory.annotation.Value("${app.admin.student-id:N/A}") // ✅ N/A
    private String adminStudentId;
    @org.springframework.beans.factory.annotation.Value("${app.admin.department:Administration}")
    private String adminDepartment;
    @org.springframework.beans.factory.annotation.Value("${app.admin.phone:N/A}")     // ✅ N/A
    private String adminPhone;

    @org.springframework.beans.factory.annotation.Value("${app.admin.lock-password:true}")
    private boolean lockPassword;

    @org.springframework.beans.factory.annotation.Value("${app.admin.privacy-mask:true}")
    private boolean privacyMask;

    @Override
    public void run(String... args) {
        if (isBlank(adminEmail) || isBlank(adminPassword)) {
            log.warn("Admin bootstrap skipped: email/password not configured.");
            return;
        }

        var existing = userRepository.findByEmail(adminEmail).orElse(null);


        if (privacyMask) {
            adminName = "System Admin";
            adminStudentId = "N/A";
            adminDepartment = "Administration";
            adminPhone = "N/A";
        }

        if (existing != null && !(existing instanceof com.diu.ridesharing.entity.Admin)) {
            userRepository.delete(existing);
            existing = null;
            log.info("Existing admin row with non-ADMIN USER_TYPE removed; will recreate as ADMIN type.");
        }

        if (existing == null) {

            Admin admin = new Admin();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);

            admin.setStatus("ACTIVE");
            admin.setName(adminName);
            admin.setStudentId(adminStudentId);
            admin.setDepartment(adminDepartment);
            admin.setPhone(adminPhone);

            userRepository.save(admin);
            log.info("Admin user CREATED with USER_TYPE=ADMIN, ROLE=ADMIN: {}", adminEmail);
            return;
        }


        if (lockPassword) {
            existing.setPassword(passwordEncoder.encode(adminPassword));
        }
        existing.setName(adminName);
        existing.setStudentId(adminStudentId);
        existing.setDepartment(adminDepartment);
        existing.setPhone(adminPhone);

        if (existing.getRole() != User.Role.ADMIN) {
            existing.setRole(User.Role.ADMIN);
        }
        if (existing.getStatus() == null || existing.getStatus().isBlank()) {
            existing.setStatus("ACTIVE");
        }

        userRepository.save(existing);
        log.info("Admin user ENSURED with USER_TYPE=ADMIN, ROLE=ADMIN: {} (password {}reset; privacyMask={})",
                adminEmail, lockPassword ? "" : "NOT ", privacyMask);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
