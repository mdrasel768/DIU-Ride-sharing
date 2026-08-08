package com.diu.ridesharing.service;

import com.diu.ridesharing.entity.Driver;
import com.diu.ridesharing.entity.User;
import com.diu.ridesharing.repository.DriverRepository;
import com.diu.ridesharing.repository.UserRepository;
import com.diu.ridesharing.security.AdminEmailRoleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final AdminEmailRoleResolver adminEmailRoleResolver;

    @PreAuthorize("hasRole('ADMIN')")
    public User banUser(Long userId, String reason) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));


        if (adminEmailRoleResolver.isAdminEmail(u.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot ban admin account");
        }

        u.setStatus("BANNED");
        return userRepository.save(u);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User unbanUser(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));


        if (adminEmailRoleResolver.isAdminEmail(u.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin account does not need unban");
        }

        u.setStatus("ACTIVE");
        return userRepository.save(u);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Page<User> listUsers(Pageable pageable, String role) {
        Page<User> page = userRepository.findAll(pageable);

        Stream<User> stream = page.getContent().stream()
                .filter(u -> u != null && !adminEmailRoleResolver.isAdminEmail(u.getEmail()));

        if (StringUtils.hasText(role)) {
            String rn = role.trim().toUpperCase();
            stream = stream.filter(u -> u.getRole() != null && u.getRole().name().equals(rn));
        }

        List<User> filtered = stream.toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Driver verifyDriver(Long driverId) {
        Driver d = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));

        if (!StringUtils.hasText(d.getLicenseNumber()) || !StringUtils.hasText(d.getBikeNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Missing licenseNumber or bikeNumber"
            );
        }

        d.setStatus("VERIFIED");
        return driverRepository.save(d);
    }
}
