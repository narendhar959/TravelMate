package com.klu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    private final Map<String, String> otpStore = new HashMap<>();

    public void generateAndSendOtp(String email) throws Exception {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        System.out.println("[OTP] Generated OTP for: " + email);

        otpStore.put(email, otp);

        sendViaBrevoApi(email, otp);
        System.out.println("[OTP] Email sent via Brevo API to: " + email);
    }

    private void sendViaBrevoApi(String toEmail, String otp) throws Exception {
        String body = "{"
            + "\"sender\":{\"name\":\"TravelMate\",\"email\":\"" + senderEmail + "\"},"
            + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
            + "\"subject\":\"Your OTP Code - TravelMate\","
            + "\"htmlContent\":\"<h2 style='color:#008080'>TravelMate OTP Verification</h2>"
            + "<p>Your one-time password is:</p>"
            + "<h1 style='letter-spacing:8px;color:#333'>" + otp + "</h1>"
            + "<p>This OTP is valid for <strong>10 minutes</strong>.</p>"
            + "<p style='color:#999;font-size:12px'>Do not share this OTP with anyone.</p>\""
            + "}";

        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", brevoApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200 && status != 201) {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            throw new RuntimeException("Brevo API error " + status + ": " + err);
        }
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
