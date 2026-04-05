package com.klu.controller;

import com.klu.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/assistant-chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = aiService.handleChat(request);
        if (result.containsKey("error") && !result.containsKey("reply")) {
            return ResponseEntity.badRequest().body(result);
        }
        boolean created = Boolean.TRUE.equals(result.get("booking_created"));
        return created ? ResponseEntity.status(201).body(result) : ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("status", "running", "service", "travelmate-ai"));
    }
}
