package com.klu.service;

import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    public void sendOtp(String toEmail, String otp) {
        try {
            String apiKey = System.getenv("RESEND_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                System.err.println("[EmailService] ERROR: RESEND_API_KEY is not set");
                return;
            }

            String json = "{"
                    + "\"from\": \"onboarding@resend.dev\","
                    + "\"to\": [\"" + toEmail + "\"],"
                    + "\"subject\": \"Your TravelMate OTP Code\","
                    + "\"html\": \"<h2>Your OTP is: <strong>" + otp + "</strong></h2><p>This OTP is valid for 10 minutes.</p>\""
                    + "}";

            HttpURLConnection conn = (HttpURLConnection) new URL("https://api.resend.com/emails").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                System.out.println("[EmailService] OTP email sent to " + toEmail + " | status: " + responseCode);
            } else {
                InputStream err = conn.getErrorStream();
                String errorBody = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "no body";
                System.err.println("[EmailService] Failed to send email | status: " + responseCode + " | body: " + errorBody);
            }

        } catch (Exception e) {
            System.err.println("[EmailService] Exception while sending OTP email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
