package com.diu.ridesharing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;

    // from address = spring.mail.username
    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final int TTL_MINUTES = 5;          // OTP valid for 5 minutes
    private static final int VERIFIED_TTL_MIN = 10;    // “recently verified” window
    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final Map<String, Instant> recentlyVerified = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public void sendRegistrationOtp(String rawEmail) {
        String email = normalize(rawEmail);
        sendOtp(email, "Your DIU Ride Sharing registration OTP is: ");
    }

    public boolean verifyRegistrationOtp(String rawEmail, String code) {
        String email = normalize(rawEmail);
        boolean ok = verifyOtpInternal(email, code);
        if (ok) {

            recentlyVerified.put(email, Instant.now().plus(VERIFIED_TTL_MIN, ChronoUnit.MINUTES));
        }
        return ok;
    }

    public void assertRecentlyVerified(String rawEmail) {
        String email = normalize(rawEmail);
        Instant until = recentlyVerified.get(email);
        if (until == null || Instant.now().isAfter(until)) {
            throw new IllegalStateException("Email OTP not verified for registration");
        }

    }


    private void sendOtp(String email, String messagePrefix) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        store.put(email, new OtpEntry(code, Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES)));

        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("Your OTP Code");
        msg.setText(messagePrefix + code + "\nThis code expires in " + TTL_MINUTES + " minutes.");

        try {
            mailSender.send(msg);
        } catch (MailAuthenticationException e) {

            throw new RuntimeException("Mail authentication failed. Check SMTP username/password.", e);
        } catch (MailException e) {

            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    private boolean verifyOtpInternal(String email, String code) {
        OtpEntry entry = store.get(email);
        if (entry == null) return false;

        if (Instant.now().isAfter(entry.expiresAt)) {
            store.remove(email);
            return false;
        }
        if (entry.attempts >= MAX_ATTEMPTS) {
            store.remove(email);
            return false;
        }
        entry.attempts++;

        boolean ok = entry.code.equals(code);
        if (ok) {
            store.remove(email); // consume on success
        }
        return ok;
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static class OtpEntry {
        final String code;
        final Instant expiresAt;
        int attempts = 0;
        OtpEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}
