package com.klu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    private final Map<String, String> otpStore = new HashMap<>();

    public void generateAndSendOtp(String email) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        System.out.println("[OTP] Generated OTP: " + otp + " for: " + email);

        otpStore.put(email, otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("narendrasanka959@gmail.com");
        message.setTo(email);
        message.setSubject("Your OTP Code - TravelMate");
        message.setText("Your OTP is: " + otp + "\n\nThis OTP is valid for 10 minutes.\n\nDo not share this OTP with anyone.");

        mailSender.send(message);
        System.out.println("[OTP] Email sent successfully to: " + email);
    }

    public boolean verifyOtp(String email, String otp) {
        String stored = otpStore.get(email);
        if (stored != null && stored.equals(otp)) {
            otpStore.remove(email);
            return true;
        }
        return false;
    }
}
