package com.diu.ridesharing.service;

import com.diu.ridesharing.dto.AuthRequest;
import com.diu.ridesharing.dto.AuthResponse;
import com.diu.ridesharing.dto.ForgotPasswordRequest;
import com.diu.ridesharing.dto.RegistrationRequest;
import com.diu.ridesharing.dto.ResetPasswordRequest;
import com.diu.ridesharing.entity.Driver;
import com.diu.ridesharing.entity.Rider;
import com.diu.ridesharing.entity.User;
import com.diu.ridesharing.exception.EmailAlreadyExistsException;
import com.diu.ridesharing.exception.StudentIdAlreadyExistsException;
import com.diu.ridesharing.repository.UserRepository;
import com.diu.ridesharing.security.AdminEmailRoleResolver;
import com.diu.ridesharing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final AdminEmailRoleResolver adminEmailRoleResolver;
    private final EmailService emailService;

    public AuthResponse login(AuthRequest authRequest) {
        final String email = authRequest.getEmail();

        userRepository.findByEmail(email).ifPresent(u -> {
            String st = (u.getStatus() == null) ? "" : u.getStatus().trim().toUpperCase();
            if ("BANNED".equals(st) || "SUSPENDED".equals(st)) {
                throw new DisabledException("Account is " + st + ". Please contact admin.");
            }
        });

        Authentication baseAuth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, authRequest.getPassword())
        );

        Authentication finalAuth = baseAuth;
        if (adminEmailRoleResolver.isAdminEmail(email)) {
            finalAuth = new UsernamePasswordAuthenticationToken(
                    baseAuth.getPrincipal(),
                    baseAuth.getCredentials(),
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        SecurityContextHolder.getContext().setAuthentication(finalAuth);
        String token = jwtTokenProvider.generateToken(finalAuth);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        AuthResponse response = new AuthResponse(token, user);

        if (adminEmailRoleResolver.isAdminEmail(email)) {
            response.setRole("ADMIN");
        }

        return response;
    }

    public AuthResponse register(RegistrationRequest req) {
        final String email = req.getEmail();

        otpService.assertRecentlyVerified(email);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (userRepository.existsByStudentId(req.getStudentId())) {
            throw new StudentIdAlreadyExistsException("Student ID already exists");
        }

        boolean hasLicense = notBlank(req.getLicenseNumber());
        boolean hasBike = notBlank(req.getBikeNumber());

        if ((hasLicense || hasBike) && !(hasLicense && hasBike)) {
            throw new IllegalArgumentException(
                    "Driver হতে হলে License Number এবং Bike Number—দুটিই দিতে হবে"
            );
        }

        User user;
        if (hasLicense && hasBike) {
            Driver d = new Driver();
            d.setLicenseNumber(req.getLicenseNumber().trim());
            d.setBikeNumber(req.getBikeNumber().trim());
            d.setRole(User.Role.DRIVER);
            user = d;
        } else {
            Rider r = new Rider();
            r.setRole(User.Role.RIDER);
            user = r;
        }

        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setStudentId(req.getStudentId());
        user.setDepartment(req.getDepartment());
        user.setStatus("ACTIVE");

        userRepository.save(user);

        Authentication baseAuth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.getPassword())
        );

        Authentication finalAuth = adminEmailRoleResolver.isAdminEmail(email)
                ? new UsernamePasswordAuthenticationToken(
                baseAuth.getPrincipal(),
                baseAuth.getCredentials(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        )
                : baseAuth;

        SecurityContextHolder.getContext().setAuthentication(finalAuth);
        String token = jwtTokenProvider.generateToken(finalAuth);

        AuthResponse response = new AuthResponse(token, user);

        if (adminEmailRoleResolver.isAdminEmail(email)) {
            response.setRole("ADMIN");
        }

        return response;
    }

    public String forgotPassword(ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(expiry);
        userRepository.save(user);

        String resetLink = "http://localhost:8080/reset-password.html?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        return "Password reset link sent to your email.";
    }

    public String resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByResetPasswordToken(req.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (user.getResetPasswordTokenExpiry() == null ||
                user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);

        return "Password reset successful";
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}