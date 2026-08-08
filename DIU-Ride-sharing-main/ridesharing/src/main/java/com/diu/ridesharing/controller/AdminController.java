package com.diu.ridesharing.controller;

import com.diu.ridesharing.dto.UserRowDTO;
import com.diu.ridesharing.entity.Driver;
import com.diu.ridesharing.entity.User;
import com.diu.ridesharing.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> ban(@PathVariable Long id,
                                                   @RequestParam(required = false) String reason) {
        User u = adminService.banUser(id, reason);
        return ResponseEntity.ok(Map.of("userId", u.getId(), "status", u.getStatus()));
    }

    @PatchMapping("/users/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> unban(@PathVariable Long id) {
        User u = adminService.unbanUser(id);
        return ResponseEntity.ok(Map.of("userId", u.getId(), "status", u.getStatus()));
    }


    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserRowDTO> list(org.springframework.data.domain.Pageable pageable,
                                 @RequestParam(required = false) String role) {
        Page<User> page = adminService.listUsers(pageable, role);
        return page.map(UserRowDTO::from);
    }

    @PatchMapping("/drivers/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable Long id) {
        Driver d = adminService.verifyDriver(id);
        return ResponseEntity.ok(Map.of("driverId", d.getId(), "status", d.getStatus()));
    }
}
