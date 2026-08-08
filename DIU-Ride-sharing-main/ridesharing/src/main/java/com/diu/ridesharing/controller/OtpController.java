package com.diu.ridesharing.controller;

import com.diu.ridesharing.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email required"));
        }
        otpService.sendRegistrationOtp(email);
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + email));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String code  = req.get("code");
        if (email == null || email.trim().isEmpty() || code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("verified", false, "error", "Email & code required"));
        }
        boolean ok = otpService.verifyRegistrationOtp(email, code);
        return ok
                ? ResponseEntity.ok(Map.of("verified", true))
                : ResponseEntity.badRequest().body(Map.of("verified", false, "error", "Invalid or expired OTP"));
    }
}
