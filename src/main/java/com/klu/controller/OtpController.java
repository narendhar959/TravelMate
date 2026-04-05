package com.klu.controller;

import com.klu.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody Map<String, String> body) {
        Map<String, String> response = new HashMap<>();
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            response.put("status", "error");
            response.put("message", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }

        System.out.println("[OTP] Received request for email: " + email);

        try {
            otpService.generateAndSendOtp(email);
            System.out.println("[OTP] OTP sent successfully to: " + email);
            response.put("status", "success");
            response.put("message", "OTP sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[OTP] Failed to send OTP: " + e.getMessage());
            response.put("status", "error");
            response.put("message", "Failed to send OTP: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> body) {
        Map<String, String> response = new HashMap<>();
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            response.put("status", "error");
            response.put("message", "Email and OTP are required");
            return ResponseEntity.badRequest().body(response);
        }

        System.out.println("[OTP] Verifying OTP for email: " + email);

        if (otpService.verifyOtp(email, otp)) {
            System.out.println("[OTP] OTP verified successfully for: " + email);
            response.put("status", "success");
            response.put("message", "OTP verified successfully");
            return ResponseEntity.ok(response);
        }

        System.out.println("[OTP] Invalid OTP attempt for: " + email);
        response.put("status", "error");
        response.put("message", "Invalid or expired OTP");
        return ResponseEntity.badRequest().body(response);
    }
}
