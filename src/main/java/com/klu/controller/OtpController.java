package com.klu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class OtpController {

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, String> otpStore = new HashMap<>();

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        Map<String, String> response = new HashMap<>();

        if (email == null || email.isBlank()) {
            response.put("error", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }

        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        otpStore.put(email, otp);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TravelMate OTP Verification");
            message.setText("Your OTP is: " + otp + "\n\nThis OTP is valid for 10 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            response.put("error", "Failed to send OTP email: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        response.put("message", "OTP sent to " + email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> data) {
        String email = data.get("email");
        String otp = data.get("otp");
        Map<String, String> response = new HashMap<>();

        if (otp != null && otp.equals(otpStore.get(email))) {
            otpStore.remove(email);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        }

        response.put("error", "Invalid or expired OTP");
        return ResponseEntity.badRequest().body(response);
    }
}
